package org.lizhao.validator.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import org.lizhao.validator.entity.AlarmRegex;
import org.lizhao.validator.entity.Device;
import org.lizhao.validator.exception.CustomException;
import org.lizhao.validator.exception.PatternNotMatchException;
import org.lizhao.validator.model.*;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.lizhao.validator.enums.DeviceTypeEnum;
import org.lizhao.validator.mapper.AlarmRegexMapper;
import org.lizhao.validator.mapper.AlarmRuleMapper;
import org.lizhao.validator.mapper.AlarmTypeMapper;
import org.lizhao.validator.mapper.DeviceMapper;
import org.lizhao.validator.event.DeviceFlushEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class AlarmService {

    /**
     * checkInfo = device.getName() + checkInfo;
     * 如： index + 告警信息 + 匹配到的 设备  + 规范信息（规范信息类型）
     */
    private final static boolean PRINT_MATCH_PATTERN = true;

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DataService dataService;

    @Resource
    private AlarmRegexMapper deviceSCADARegexMapper;

    @Resource
    private AlarmTypeMapper deviceSCADAInfoTypeMapper;

    @Resource
    private AlarmRuleMapper deviceSCADAInfoRuleMapper;

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;


    // 电压 正则 \d*(?i:kv)
    private final String voltagePattern = "\\d+[kK]?[vV]";

    // 主变 正则
    private final String mainTransformerPattern = "#\\d+主变";

    // 母线 正则 注意排在“线路正则”前，否则匹配到线路 (?:.(?!开关|开关间隔|电[容|抗]器组))*母线
    private final String generatrixPattern = "#\\d+?(?:母线|(?=母差))|#\\d+(?=电压互感器\\d+PT)";

    // 双母线 母联 分段 \d+M\d+M(?:母线|)
    private final String doubleGeneratrixPattern = "#\\d+母线#\\d+母线|(?:母联|分段)(?=\\d+(?=开关|刀闸|隔离小车))";

    // 线高抗 正则
    private final String lineHighResistancePattern = ".*线高抗";

    // 线路 正则 （排除 开关）
    // (?:[^线](?!开关|开关间隔|电[容|抗]器组|母线|小电流))*(?!消弧)线(?!圈)
    // (?:.(?!开关|开关间隔|电[容|抗]器组|母线|小电流|消弧线圈))*?(?!消弧)线(?!圈)
    // (?!.*线高抗)(?:.(?!开关|开关间隔|电[容|抗]器组|母线|小电流))*?(?!消弧)线(?!圈)
    // (?!.*线高抗)(?!.*母线)(?:.(?!开关|开关间隔|电[容|抗]器组|母线|小电流))*?(?!消弧)线(?!圈)
    private final String linePattern = "(?!.*线高抗)(?:.(?!开关|开关间隔|电[容|抗]器组|母线|小电流))*?(?<!消弧|\\d母|[ⅠⅡ]段交流馈)线(?!圈|路)";

    // 电容器组/电抗器组 正则
    private final String capacitanceAndReactancePattern= "(?:#\\d+(?:低压电[容|抗]器|电[容|抗]器组)|AVC(?:#\\d+(?:低压电容器|电容器组))?)";

    // 站用变
    private final String stationTransformerPattern= "#\\d+站用变(?=兼接地变).{0,4}|#\\d+站用变";

    // 曲折变/接地变 正则
    private final String zigzagAndGroundTransformerPattern = "#\\d+(?:曲折|接地)变";

    // 站用电
    private final String stationPowerPattern= " ?站用电";

    // 开关 \d*开关(?=间隔).{2,}|\d*开关
    private final String switchButtonPattern = "(?!.*路.*开关).+?开关(?=间隔|)|(?=\\d+\\d刀闸|刀闸气室SF6)|\\d+[A-Z0-9]\\d接地刀闸";
//    private final String switchButtonPattern = ".+?开关(?=间隔|)|(?=\\d+\\d刀闸|刀闸气室SF6)";
    private final String switchButtonCodePattern = "(.+)开关(?=间隔).{0,2}|(.+)开关";

    // 消弧线圈
    private final String petersonColiPattern = "#\\d+消弧线圈";

    // 小电阻
    private final String resistorPattern = "#\\d+小电阻";

    private final String dcSystemPattern = "(#\\d+)?直流系统";

    private final String stabilityControl = "稳控装置";

    private final String backupAutomaticSwitchPattern = "备自投装置";

    private final String outOfStepSeparationPattern = ".*失步解列";

    private final String frequencyVoltageEmergencyControlPattern = "频率电压紧急控制装置";

    private final String vqcPattern = "VQC";

    private final String communicationPowerControlPattern = "通信电源";

    private final String smallCurrentPattern = "\\d+小电流接地选线装置";
    // 其他
    private final String info = ".*";

    private Pattern pattern;

    private final List<RegexPattern> regexPatterns = new ArrayList<>();

    private Map<String, List<Device>> deviceMap = new HashMap<>();

    private Map<String, Device> deviceIdMap = new HashMap<>();

    private Map<String, AlarmRegex> deviceRegexMap = new HashMap<>();

    private final Map<String, Map<DeviceTypeEnum, List<LinkedList<DeviceTypeEnum>>>> deviceRelationMap = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        // 主变开关
        LinkedList<DeviceTypeEnum> mainSwitchRelations = new LinkedList<>();
        mainSwitchRelations.add(DeviceTypeEnum.MAIN_TRANSFORMER);
        mainSwitchRelations.add(DeviceTypeEnum.SWITCH);

        addRelationMap("500kV", mainSwitchRelations);
        addRelationMap("220kV", mainSwitchRelations);
        addRelationMap("110kV", mainSwitchRelations);

        // 线路开关
        LinkedList<DeviceTypeEnum> lineSwitchRelations = new LinkedList<>();
        lineSwitchRelations.add(DeviceTypeEnum.LINE);
        lineSwitchRelations.add(DeviceTypeEnum.SWITCH);

        addRelationMap("500kV", lineSwitchRelations);
        addRelationMap("220kV", lineSwitchRelations);
        addRelationMap("110kV", lineSwitchRelations);
        addRelationMap("35kV", lineSwitchRelations);
        addRelationMap("10kV", lineSwitchRelations);

        // 母联开关 分段开关
        LinkedList<DeviceTypeEnum> generatrixSwitchRelations = new LinkedList<>();
        generatrixSwitchRelations.add(DeviceTypeEnum.DOUBLE_GENERATRIX);
        generatrixSwitchRelations.add(DeviceTypeEnum.SWITCH);

        addRelationMap("220kV", generatrixSwitchRelations);
        addRelationMap("110kV", generatrixSwitchRelations);
        addRelationMap("10kV", generatrixSwitchRelations);

        // 电容器组开关
        LinkedList<DeviceTypeEnum> capacityAndReactiveSwitchRelations = new LinkedList<>();
        capacityAndReactiveSwitchRelations.add(DeviceTypeEnum.CAPACITANCE_AND_REACTANCE);
        capacityAndReactiveSwitchRelations.add(DeviceTypeEnum.SWITCH);

        addRelationMap("66kV", capacityAndReactiveSwitchRelations);
        addRelationMap("35kV", capacityAndReactiveSwitchRelations);
        addRelationMap("20kV", capacityAndReactiveSwitchRelations);
        addRelationMap("10kV", capacityAndReactiveSwitchRelations);

        // 接地变开关
        LinkedList<DeviceTypeEnum> groundTransformerRelations = new LinkedList<>();
        groundTransformerRelations.add(DeviceTypeEnum.ZIGZAG_AND_GROUND_TRANSFORMER);
        groundTransformerRelations.add(DeviceTypeEnum.SWITCH);

        addRelationMap("10kV", groundTransformerRelations);

        // 站用变开关
        LinkedList<DeviceTypeEnum> stationTransformerRelations = new LinkedList<>();
        stationTransformerRelations.add(DeviceTypeEnum.STATION_TRANSFORMER);
        stationTransformerRelations.add(DeviceTypeEnum.SWITCH);

        addRelationMap("10kV", stationTransformerRelations);

        // 曲折变/接地变 开关
//        LinkedList<DeviceTypeEnum> zigzagTransformerSwitchRelations = new LinkedList<>();
//        zigzagTransformerSwitchRelations.add(DeviceTypeEnum.GROUND_TRANSFORMER);
//        zigzagTransformerSwitchRelations.add(DeviceTypeEnum.SWITCH);
//
//        addRelationMap("10kV", zigzagTransformerSwitchRelations);

        // 10kV 分段-开关-备自投
        LinkedList<DeviceTypeEnum> doubleGeneratrixSwitchBackupAutomaticSwitchRelations = new LinkedList<>();
        doubleGeneratrixSwitchBackupAutomaticSwitchRelations.add(DeviceTypeEnum.DOUBLE_GENERATRIX);
        doubleGeneratrixSwitchBackupAutomaticSwitchRelations.add(DeviceTypeEnum.SWITCH);
        doubleGeneratrixSwitchBackupAutomaticSwitchRelations.add(DeviceTypeEnum.BACKUP_AUTOMATIC_SWITCH);
        addRelationMap("10kV", doubleGeneratrixSwitchBackupAutomaticSwitchRelations);



        // 设备匹配正则 生成
        RegexPattern voltageRegexPattern = new RegexPattern(voltagePattern, "voltage");
        RegexPattern vqcRegexPattern = new RegexPattern(vqcPattern, DeviceTypeEnum.VQC);
        RegexPattern mainTransformerRegexPattern = new RegexPattern(mainTransformerPattern, DeviceTypeEnum.MAIN_TRANSFORMER);
        RegexPattern stationPowerRegexPattern = new RegexPattern(stationPowerPattern, DeviceTypeEnum.STATION_POWER);
        RegexPattern dcSystemRegexPattern = new RegexPattern(dcSystemPattern, DeviceTypeEnum.DC_SYSTEM);
        RegexPattern generatirxRegexPattern = new RegexPattern(generatrixPattern, DeviceTypeEnum.GENERATRIX);
        RegexPattern doubleGeneratrixRegexPattern = new RegexPattern(doubleGeneratrixPattern, DeviceTypeEnum.DOUBLE_GENERATRIX);
        RegexPattern lineHighResistanceRegexPattern = new RegexPattern(lineHighResistancePattern, DeviceTypeEnum.LINE_HIGH_RESISTANCE);
        RegexPattern lineRegexPattern = new RegexPattern(linePattern, DeviceTypeEnum.LINE);
        RegexPattern capacitanceAndReactanceRegexPattern = new RegexPattern(capacitanceAndReactancePattern, DeviceTypeEnum.CAPACITANCE_AND_REACTANCE);
        RegexPattern stationTransformerRegexPattern = new RegexPattern(stationTransformerPattern, DeviceTypeEnum.STATION_TRANSFORMER);
        RegexPattern zigzagAndGroundTransformerRegexPattern = new RegexPattern(zigzagAndGroundTransformerPattern, DeviceTypeEnum.ZIGZAG_AND_GROUND_TRANSFORMER);
        RegexPattern switchButtonRegexPattern = new RegexPattern(switchButtonPattern, DeviceTypeEnum.SWITCH);
        RegexPattern petersonColiRegexPattern = new RegexPattern(petersonColiPattern, DeviceTypeEnum.PETERSON_COIL);
        RegexPattern resistorRegexPattern = new RegexPattern(resistorPattern, DeviceTypeEnum.SMALL_RESISTOR);

        regexPatterns.add(voltageRegexPattern);
        regexPatterns.add(vqcRegexPattern);
        regexPatterns.add(mainTransformerRegexPattern);
        regexPatterns.add(stationPowerRegexPattern);
        regexPatterns.add(dcSystemRegexPattern);
        regexPatterns.add(doubleGeneratrixRegexPattern);
        regexPatterns.add(generatirxRegexPattern);
        regexPatterns.add(lineHighResistanceRegexPattern);
        regexPatterns.add(lineRegexPattern);
        regexPatterns.add(capacitanceAndReactanceRegexPattern);
        regexPatterns.add(stationTransformerRegexPattern);
        regexPatterns.add(zigzagAndGroundTransformerRegexPattern);
        regexPatterns.add(switchButtonRegexPattern);
        regexPatterns.add(petersonColiRegexPattern);
        regexPatterns.add(resistorRegexPattern);

        RegexPattern stabilityControlRegexPattern = new RegexPattern(stabilityControl, DeviceTypeEnum.STABILITY_CONTROL);
        regexPatterns.add(stabilityControlRegexPattern);
        RegexPattern backupAutomaticSwitchRegexPattern = new RegexPattern(backupAutomaticSwitchPattern, DeviceTypeEnum.BACKUP_AUTOMATIC_SWITCH);
        regexPatterns.add(backupAutomaticSwitchRegexPattern);
        RegexPattern outOfStepSeparationRegexPattern = new RegexPattern(outOfStepSeparationPattern, DeviceTypeEnum.OUT_OF_STEP_SEPARATION);
        regexPatterns.add(outOfStepSeparationRegexPattern);
        RegexPattern frequencyVoltageEmergencyControlRegexPattern = new RegexPattern(frequencyVoltageEmergencyControlPattern, DeviceTypeEnum.FREQUENCY_VOLTAGE_EMERGENCY_CONTROL);
        regexPatterns.add(frequencyVoltageEmergencyControlRegexPattern);

        RegexPattern communicationPowerControlRegexPattern = new RegexPattern(communicationPowerControlPattern, DeviceTypeEnum.COMMUNICATION_POWER_CONTROL);
        regexPatterns.add(communicationPowerControlRegexPattern);
        RegexPattern smallCurrentRegexPattern = new RegexPattern(smallCurrentPattern, DeviceTypeEnum.SMALL_CURRENT);
        regexPatterns.add(smallCurrentRegexPattern);

        RegexPattern infoRegexPattern = new RegexPattern(info, "info");
        regexPatterns.add(infoRegexPattern);

        StringBuilder sb = new StringBuilder();
        for (RegexPattern regexPattern : regexPatterns) {
            Object deviceTypeEnum = regexPattern.getDeviceTypeEnum();
            String groupName;
            if (deviceTypeEnum instanceof DeviceTypeEnum) {
                groupName = ((DeviceTypeEnum)deviceTypeEnum).getName();
            } else {
                groupName = (String) deviceTypeEnum;
            }
            sb.append("(?<").append(groupName).append(">").append(regexPattern.getPattern()).append(")?");
        }

        pattern = Pattern.compile(sb.toString());

        // FIXME
        flushDeviceMap(null);
        flushDeviceRegexMap(null);
    }

    private void addRelationMap(String voltage, LinkedList<DeviceTypeEnum> list) {
        Map<DeviceTypeEnum, List<LinkedList<DeviceTypeEnum>>> deviceTypeEnumListMap = deviceRelationMap.computeIfAbsent(voltage, k -> new HashMap<>());
        List<LinkedList<DeviceTypeEnum>> lists = deviceTypeEnumListMap.computeIfAbsent(list.getFirst(), k -> new LinkedList<>());
        lists.add(list);
    }

    /**
     * 检查 告警信息 设备关系
     * @return true-检查通过；
     *         false-异常
     */
    private boolean checkInfoDeviceRelation(List<Device> devices) {
        // 无设备默认通过
        if (CollectionUtils.isEmpty(devices)) {
            return true;
        }

        // 开关不能单独出现
        if (devices.size() == 1) {
            Device device = devices.get(0);
            DeviceTypeEnum deviceType = DeviceTypeEnum.of(device.getType());
            return deviceType != DeviceTypeEnum.SWITCH;
        }

        // 检查设备顺序
        Device firstDevice = devices.get(0);
        DeviceTypeEnum firstDeviceType = DeviceTypeEnum.of(firstDevice.getType());
        String voltage = Optional.ofNullable(firstDevice.getVoltage()).orElse("");
        Map<DeviceTypeEnum, List<LinkedList<DeviceTypeEnum>>> deviceTypesMap = deviceRelationMap.get(voltage);
        if (deviceTypesMap == null) {
            return false;
        }
        List<LinkedList<DeviceTypeEnum>> relations = deviceTypesMap.get(firstDeviceType);
        if (CollectionUtils.isEmpty(relations)) {
            return false;
        }
        List<DeviceTypeEnum> deviceTypes = devices.stream().map(device -> DeviceTypeEnum.of(device.getType())).collect(Collectors.toList());

        boolean flag = false;
        for (LinkedList<DeviceTypeEnum> relation : relations) {
            if (relation.size() != deviceTypes.size()) {
                continue;
            }

            flag = true;
            for (int i = 0; i < deviceTypes.size(); i++) {
                DeviceTypeEnum deviceType = relation.get(i);
                DeviceTypeEnum deviceType1 = deviceTypes.get(i);
                if (deviceType != deviceType1) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                break;
            }
        }

        return flag;
    }

    @EventListener
    private void flushDeviceMap(DeviceFlushEvent DeviceFlushEvent) {
        try {
            List<Device> devices = this.deviceMapper.selectList(Wrappers.emptyWrapper());
            this.deviceMap = devices.stream().collect(Collectors.groupingBy(Device::getType));
            this.deviceIdMap = devices.stream().collect(Collectors.toMap(Device::getId, e -> e, (e1,e2) -> e1));
        } catch (Exception e) {
            log.error("设备信息更新失败");
            throw new RuntimeException(e);
        }
    }

    @EventListener
    private void flushDeviceRegexMap(DeviceFlushEvent DeviceFlushEvent) {
        try {
            List<AlarmRegex> devices = this.deviceSCADARegexMapper.selectList(Wrappers.emptyWrapper());
            this.deviceRegexMap = devices.stream().collect(Collectors.toMap(AlarmRegex::getId, e -> e, (e1,e2) -> e1));
        } catch (Exception e) {
            log.error("设备检测正则信息更新失败");
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * 检查输入的文件中的警告信息格式石佛正确，输出校验报告(excel)
     * @param inputStream 文件输入流
     * @return 校验信息 Map<SheetInfo, List<DeviceSCADAInputInfo>>
     */
    public Map<SheetInfo, List<DeviceSCADAInputInfo>> check(InputStream inputStream, AtomicReference<ProgressModel> progressModel) {
        flushDeviceMap(null);
        flushDeviceRegexMap(null);

        Map<SheetInfo, List<DeviceSCADAInputInfo>> deviceSCADAInputInfoMap = transferExcel(inputStream);
        // 待校验数据量
        long totalInfoSize = deviceSCADAInputInfoMap.keySet().stream().mapToLong(e -> e == null || e.getTotalRowNum() == null ? 0 : e.getTotalRowNum()).sum();
        // 初始化进度数据
        initProgress(progressModel, totalInfoSize);

        List<Future<List<DeviceSCADAInputInfo>>> futures = new ArrayList<>();

        // substation---voltage
        List<Substation> allSubstation = dataService.findAllSubstation();
        Map<String, String> allSubstationVoltageMap = Optional.ofNullable(allSubstation).orElse(Collections.emptyList()).stream().collect(Collectors.toMap(Substation::getName, Substation::getVoltage, (e1,e2) -> e2));

        for (Map.Entry<SheetInfo, List<DeviceSCADAInputInfo>> entry : deviceSCADAInputInfoMap.entrySet()) {
            List<DeviceSCADAInputInfo> deviceSCADAInputInfos = entry.getValue();

            List<DeviceSCADAInputInfo> infos = extractDeviceInfo(deviceSCADAInputInfos);

            Set<Device> devices = new HashSet<>();
            for (DeviceSCADAInputInfo info : infos) {

                // 校验设备信息 编号等命名是否规范
                info.setDevicesCheckResult(true);

                if (info.getDevices() == null) {
//                    throw new RuntimeException("设备信息为空");
                    info.setDevicesCheckResult(false);
                    continue;
                }

                for (Device device : info.getDevices()) {
                    // 设置 设备id，获取获取设备下的规范信息
                    checkDevice(device, info, allSubstationVoltageMap);
                }

                if (info.getDevices().isEmpty()) {
                    Device device = new Device();
                    device.setCheckResult(true);
                    devices.add(device);
                }

                // 从最低级设备往上校验
                devices.addAll(info.getDevices());
            }

            // 校验 信息类型、规范信息
            // 1、根据设备获取 对应的设备信息
            List<AlarmTypeRuleModel> deviceInfoTypes = searchDeviceInfoType(devices);
            // 2、校验 信息
            ConcurrentSkipListSet<String> parsedInfos = new ConcurrentSkipListSet<>();
            // 多线程校验 600条 一个线程
            if (infos.size() > 600) {
                List<List<DeviceSCADAInputInfo>> lists = splitCollections(infos, 600);
                for (List<DeviceSCADAInputInfo> list : lists) {
                    futures.add(threadPoolTaskExecutor.submit(() -> checkWarnInfo(deviceInfoTypes, list, parsedInfos, progressModel)));
                }

                List<DeviceSCADAInputInfo> result = new ArrayList<>();
                for (Future<List<DeviceSCADAInputInfo>> future : futures) {
                    try {
                        result.addAll(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                entry.setValue(result);
            } else {
                entry.setValue(checkWarnInfo(deviceInfoTypes, infos, parsedInfos, progressModel));
            }
        }
        return deviceSCADAInputInfoMap;
    }

    /**
     * 按固定长度
     *
     * @param collections 列表
     * @param num 每组元素数量
     * @return 分组后列表
     */
    public static <T> List<List<T>> splitCollections(List<T> collections, int num) {
        if (CollectionUtils.isEmpty(collections)) {
            return Collections.emptyList();
        }

        if (collections.size() <= num) {
            return Collections.singletonList(collections);
        }

        List<List<T>> reList = new ArrayList<>();
        int index = 0;
        while (index < collections.size()) {
            int nextIndex = index + num;
            if (nextIndex < collections.size()) {
                reList.add(collections.subList(index, nextIndex));
            } else {
                reList.add(collections.subList(index, collections.size()));
            }
            index = nextIndex;
        }

        return reList;
    }

    /**
     * 想输出流写入生成的excel数据
     *
     * @param currentUserName 当前用户名
     * @param infoMap 信息
     * @param outputStream 输出流
     */
    public void createExcel(String currentUserName, Map<SheetInfo, List<AlarmInfo>> infoMap, OutputStream outputStream) {
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook()){
            List<SheetInfo> sheetInfos = infoMap.keySet().stream().sorted(Comparator.comparingInt(SheetInfo::getIndex)).toList();
            for (SheetInfo sheetInfo : sheetInfos) {
                XSSFSheet sheet = xssfWorkbook.createSheet(sheetInfo.getName());
                XSSFRow firstRow = sheet.createRow(0);
                XSSFCell firstCell = firstRow.createCell(0);
                firstCell.setCellValue("告警型号\"一站一册\"规范化问题清单");
                XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                firstCell.setCellStyle(cellStyle);

                // 合并 A1:F1
                CellRangeAddress region = new CellRangeAddress(0, 0, 0, 4);
                sheet.addMergedRegion(region);

//                XSSFCell secondCell = firstRow.createCell(6);
//                secondCell.setCellValue(new XSSFRichTextString("核对人：" + currentUserName + "\n" + "校核人(值班张/值班负责人)："));

                XSSFRow secondRow = sheet.createRow(1);

                secondRow.createCell(0).setCellValue("序号");
                secondRow.createCell(1).setCellValue("点号");
                secondRow.createCell(2).setCellValue("信号名称");
                secondRow.createCell(3).setCellValue("间隔");
                secondRow.createCell(4).setCellValue("检查发现问题");
//                secondRow.createCell(5).setCellValue("计划整改完成时间");
//                secondRow.createCell(6).setCellValue("实际完成整改时间");
                // TODO test
//                secondRow.createCell(7).setCellValue("耗时");

                firstRow.setHeightInPoints(60);
                secondRow.setHeightInPoints(30);
                sheet.setColumnWidth(0, 10 * 256);
                sheet.setColumnWidth(1, 10 * 256);
                sheet.setColumnWidth(2, 80 * 256);
                sheet.setColumnWidth(3, 20 * 256);
                sheet.setColumnWidth(4, 80 * 256);
//                sheet.setColumnWidth(5, 20 * 256);
//                sheet.setColumnWidth(6, 40 * 256);

                List<AlarmInfo> deviceSCADAInputInfos = infoMap.get(sheetInfo);
                for (int i = 0; i < deviceSCADAInputInfos.size(); i++) {

                    AlarmInfo inputInfo = deviceSCADAInputInfos.get(i);
                    XSSFRow row = sheet.createRow(i + 2);
                    row.createCell(0).setCellValue(i + 1);

                    XSSFCell pointCell = row.createCell(1);
                    if (StringUtils.isNotBlank(inputInfo.getPointCode())) {
                        pointCell.setCellValue(Double.parseDouble(inputInfo.getPointCode()));
                    } else {
                        pointCell.setCellValue(inputInfo.getPointCode());
                    }
                    pointCell.setCellStyle(cellStyle);

                    row.createCell(2).setCellValue(inputInfo.getAlarmContent());
                    row.createCell(3).setCellValue(inputInfo.getDevice());
                    row.createCell(4).setCellValue(inputInfo.getMessage());
                }
            }

            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 检查警告信息
     *
     * @param deviceSCADAInfoTypeRuleModels 设备警告信息规范
     * @param infos 待检查信息
     * @return 检查结果
     */
    private List<DeviceSCADAInputInfo> checkWarnInfo(List<AlarmTypeRuleModel> deviceSCADAInfoTypeRuleModels,
                                                     List<DeviceSCADAInputInfo> infos,
                                                     ConcurrentSkipListSet<String> parsedInfoSet,
                                                     AtomicReference<ProgressModel> progressModel) {
        Map<String, List<AlarmTypeRuleModel>> map = deviceSCADAInfoTypeRuleModels.stream().collect(Collectors.groupingBy(e -> e.getDeviceId() == null ? "" : e.getDeviceId()));

        // 公用告警信息
        List<AlarmTypeRuleModel> publicTypeRules = map.get("");
        // 需要全文匹配的
        List<AlarmTypeRuleModel> fullTextMatchTypeRules = deviceSCADAInfoTypeRuleModels.stream().filter(e -> BooleanUtils.isTrue(e.getFullTextMatch()) || BooleanUtils.isTrue(e.getRuleFullTextMatch())).collect(Collectors.toList());
        // 信息类型----需拆分的信息拼接
        Map<String, Map<String,String>> mergeMessageMap = deviceSCADAInfoTypeRuleModels.stream().collect(Collectors.groupingBy(e -> e.getDeviceId() == null ? "" : e.getDeviceId(), Collectors.groupingBy(e -> StringUtils.isBlank(e.getName()) ? "" : e.getName(), Collectors.mapping(AlarmTypeRuleModel::getRuleName, Collectors.joining(",")))));
        // 数据库中同一信息类型下的规范信息数量
//        Map<String, Long> ruleNumWithSameTypeInDB = deviceSCADAInfoTypeRuleModels.stream().collect(Collectors.groupingBy(DeviceSCADAInfoTypeRuleModel::getId, Collectors.counting()));

        Pattern excludePattern = Pattern.compile(".*[^接]地刀[^闸].*");
        for (DeviceSCADAInputInfo info : infos) {

            // 检查是否存在重复信号
            if (parsedInfoSet.contains(info.getWarnInfo())) {
                appendTips("重复信号", info);
                continue;
            }

            parsedInfoSet.add(info.getWarnInfo());

            // TODO test
//            LocalDateTime startTime = LocalDateTime.now();

            // TODO 跳过设备校验失败的
            if (!info.getDevicesCheckResult()) {
//                info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                continue;
            }

            List<Device> devices = info.getDevices();
            if (CollectionUtils.isNotEmpty(devices)) {
                String checkInfo = Optional.ofNullable(info.getWarnInfo()).orElse(Strings.EMPTY).replaceFirst(Optional.ofNullable(info.getVoltage()).orElse(Strings.EMPTY), "");
                boolean isMatch = false;
                for (int i = 0; i < devices.size(); i++) {
                    List<Device> subDevices = devices.subList(0, i + 1);
                    if (!checkInfoDeviceRelation(subDevices)) {
                        log.error("设备关系校验失败：{}", subDevices.stream().map(Device::getName).collect(Collectors.joining("-")));
//                        info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                        continue;
                    }

                    Device device = devices.get(i);
                    for (Device subDevice : subDevices) {
                        if (StringUtils.isBlank(subDevice.getName()) || !checkInfo.startsWith(subDevice.getName())) {
//                            info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                            continue;
                        }
                        checkInfo = checkInfo.replaceFirst(Optional.of(subDevice.getName()).orElse(Strings.EMPTY), "");
                    }

                    if (BooleanUtils.isNotTrue(device.getCheckResult())) {
                        appendTips(device.getCheckResultMessage(), info);
//                        info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                        continue;
                    }

                    List<AlarmTypeRuleModel> infoTypeFormatModels = new ArrayList<>();
                    infoTypeFormatModels.addAll(Optional.ofNullable(map.get(device.getId())).orElse(Collections.emptyList()));
                    // 加上公共的
                    infoTypeFormatModels.addAll(publicTypeRules);
                    // 加上需要全文匹配的
                    infoTypeFormatModels.addAll(fullTextMatchTypeRules);
                    // 等待校验规则为空
                    if (CollectionUtils.isEmpty(infoTypeFormatModels)) {
//                        appendTips("未找到设备对应规范信息，告警信息:" + info.getWarnInfo() + ",解析后设备名:" + device.getName(), info);
//                        appendTips("未找到设备对应规范信息，解析后设备名:" + device.getName(), info);
                        // TODO test
//                        checkInfo = device.getName() + checkInfo;
//                        info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                        continue;
                    }

                    info.setRuleCount(infoTypeFormatModels.size());

                    isMatch = matchPatternsWhichUnderDevice(device, i, infoTypeFormatModels, info, checkInfo, mergeMessageMap);

                    if (!isMatch && i == devices.size() - 1 && device.getChildId() != null) {// 尝试匹配 子设备 的警告信息
                        Device childDevice = deviceIdMap.get(device.getChildId());
                        if (childDevice != null) {
                            isMatch = matchPatternsWhichUnderDevice(childDevice, -1, map.get(childDevice.getId()), info, checkInfo, mergeMessageMap);
                        }
                    }
                    // 匹配上，跳出
                    if (isMatch) {
//                        info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                        break;
                    }
                }
                // 都没匹配上，未配置
                if (!isMatch) {
                    Matcher excludeMatcher = excludePattern.matcher(Optional.ofNullable(info.getWarnInfo()).orElse(Strings.EMPTY));
                    if (excludeMatcher.matches() || info.getWarnInfo().contains("接地开关")) {
                        appendTips("“地刀”、“接地开关”改为“接地刀闸”", info);
                    } else {
                        //测试用
//                    appendTips("告警信息：" + info.getInfo() + "，电压:" + info.getVoltage() + "，未匹配上现有规范信息", info);
                        appendTips("未匹配上现有规范信息", info);
                    }
//                    info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
                }
            } else {
                // 测试用
//                appendTips("告警信息：" + info.getInfo() + "，设备未匹配", info);
                List<AlarmTypeRuleModel> rules = map.get("");
                // 加上需要全文匹配的
                rules.addAll(fullTextMatchTypeRules);
                if (CollectionUtils.isNotEmpty(rules)) {
                    boolean checkResult = matchPatternsWhichUnderDevice(null, -1, rules, info, info.getWarnInfo(), mergeMessageMap);
                    if (!checkResult) {
                        info.setTips(Collections.singletonList("未匹配上现有规范信息"));
                    }
                }
//                info.setCost(Duration.between(startTime, LocalDateTime.now()).toMillis());
            }

            stepProgress(progressModel,1);
        }

        // 检测信息中同一信息类型下的规范信息数量
//        Map<String, Long> ruleNumWithSameTypeInData = deviceSCADAInfoTypeRuleModels.stream().collect(Collectors.groupingBy(DeviceSCADAInfoType::getId, Collectors.counting()));
        // 校验信息是否可以主站合并
        for (DeviceSCADAInputInfo deviceSCADAInputInfo : infos) {
            String infoTypeId = deviceSCADAInputInfo.getInfoTypeId();
            Boolean masterMerge = deviceSCADAInputInfo.getMasterMerge();

            // 跳过未匹配上的
            if (infoTypeId == null || masterMerge == null) {
                continue;
            }

            // 跳过 不能主站合并的
            if (!masterMerge) {
                continue;
            }

            // 跳过 相同信息类型下只有一条规范信息的 提示合并的必须展示类型
//            if (ruleNumWithSameTypeInData.get(infoTypeId) == 1L || ruleNumWithSameTypeInDB.get(infoTypeId) == 1L) {
//                continue;
//            }
            if (BooleanUtils.isTrue(deviceSCADAInputInfo.getMatchType())) {
                continue;
            }

            appendTips("符合规范，应合并为：" + deviceSCADAInputInfo.getInfoTypeName(), deviceSCADAInputInfo);
        }

        return infos;
    }

    private boolean matchPatternsWhichUnderDevice(Device device, int deviceIndex, List<AlarmTypeRuleModel> infoTypeRuleModels, DeviceSCADAInputInfo info, String checkInfo, Map<String, Map<String,String>> mergeMessageMap) {

        device = device == null ? new Device() : device;

        // 解析变量
        Map<String, String> variablesMap = new HashMap<>();
        if (deviceIndex >= 0) {
            variablesMap.put("deviceName", device.getName());
            variablesMap.put("deviceName-1", deviceIndex - 1 >= 0 && deviceIndex - 1 < info.getDevices().size() ? info.getDevices().get(deviceIndex - 1).getName() : "");
        }


        for (AlarmTypeRuleModel infoTypeRuleModel : infoTypeRuleModels) {

            // FIXME 测试用 上线注释
//            if (Optional.ofNullable(infoTypeRuleModel.getName()).orElse(Strings.EMPTY).contains("继电器")) {
//                System.out.println();
//            }
            String ruleCheckInfo = checkInfo;
            if (BooleanUtils.isTrue(infoTypeRuleModel.getRuleIncludeDeviceName())
                && Objects.equals(device.getId(), infoTypeRuleModel.getDeviceId())) {
                    ruleCheckInfo = Optional.ofNullable(device.getName()).orElse(Strings.EMPTY) + checkInfo;
            }
            ruleCheckInfo = BooleanUtils.isTrue(infoTypeRuleModel.getRuleFullTextMatch()) ? info.getWarnInfo() : ruleCheckInfo;
            // 匹配 规范信息
            // 1、pattern_id为空时，进行规范信息名称匹配
            if (StringUtils.isBlank(infoTypeRuleModel.getRulePatternId()) && Objects.equals(ruleCheckInfo, infoTypeRuleModel.getRuleDesc())) {
                info.setMatchType(true);
                info.setInfoTypeId(infoTypeRuleModel.getId());
                info.setInfoTypeName(infoTypeRuleModel.getName());
                info.setMasterMerge(infoTypeRuleModel.getMasterMerge());

                if (PRINT_MATCH_PATTERN) {
                    log.info("{}, {}{}-{}", info.getWarnInfo(), device.getVoltage(), device.getName(), infoTypeRuleModel.getRuleName());
                }
                return true;
            }

            // 匹配 规范信息
            // 2、pattern 匹配
            if (StringUtils.isNotBlank(infoTypeRuleModel.getRulePatternId())) {
                try {
                    String regexMessage = matcher(infoTypeRuleModel.getRulePatternId(), ruleCheckInfo, variablesMap);
                    // 添加检测信息
                    appendTips(regexMessage, info);
                    info.setInfoTypeId(infoTypeRuleModel.getId());
                    info.setInfoTypeName(infoTypeRuleModel.getName());
                    info.setMasterMerge(infoTypeRuleModel.getMasterMerge());
                    if (PRINT_MATCH_PATTERN) {
                        log.info("{}, {}{}-{}", info.getWarnInfo(), device.getVoltage(), device.getName(), infoTypeRuleModel.getRuleName());
                    }
                    return true;
                } catch (RuntimeException e) {
                    // do nothing
                    if(!(e instanceof PatternNotMatchException)) {
                        log.error("状况外异常", e);
                    }
                }
            }

            String typeCheckInfo = checkInfo;
            if (BooleanUtils.isTrue(infoTypeRuleModel.getIncludeDeviceName())
                && Objects.equals(device.getId(), infoTypeRuleModel.getDeviceId())) {
                typeCheckInfo = Optional.ofNullable(device.getName()).orElse(Strings.EMPTY) + checkInfo;
            }
            typeCheckInfo = BooleanUtils.isTrue(infoTypeRuleModel.getFullTextMatch()) ? info.getWarnInfo() : typeCheckInfo;
            // 匹配 信息类型
            // 1、信息类型pattern_id为空时，进行信息类型名称匹配
            if (StringUtils.isBlank(infoTypeRuleModel.getPatternId()) && Objects.equals(typeCheckInfo, infoTypeRuleModel.getName())) {
                info.setMatchType(true);
                info.setInfoTypeId(infoTypeRuleModel.getId());
                info.setInfoTypeName(infoTypeRuleModel.getName());
                info.setMasterMerge(infoTypeRuleModel.getMasterMerge());
                if (!infoTypeRuleModel.getMasterMerge()) {
                    Map<String, String> deviceMergeMessageMap = mergeMessageMap.get(device.getId());
                    // FIXME device.getId() == null
                    appendTips("符合规范，应拆分为：" + deviceMergeMessageMap.get(infoTypeRuleModel.getName()), info);
                }
                if (PRINT_MATCH_PATTERN) {
                    log.info("{}, {}{}-{}", info.getWarnInfo(), device.getVoltage(), device.getName(), infoTypeRuleModel.getName());
                }
                return true;
            }

            // 2、pattern 匹配
            if (StringUtils.isNotBlank(infoTypeRuleModel.getPatternId())) {
                try {
                    String regexMessage = matcher(infoTypeRuleModel.getPatternId(), typeCheckInfo, variablesMap);
                    // 添加检测信息
                    appendTips(regexMessage, info);
                    info.setInfoTypeId(infoTypeRuleModel.getId());
                    info.setInfoTypeName(infoTypeRuleModel.getName());
                    info.setMasterMerge(infoTypeRuleModel.getMasterMerge());
                    if (PRINT_MATCH_PATTERN) {
                        log.info("{}, {}{}-{}", info.getWarnInfo(), device.getVoltage(), device.getName(), infoTypeRuleModel.getName());
                    }
                    return true;
                } catch (RuntimeException e) {
                    // do nothing
                    if(!(e instanceof PatternNotMatchException)) {
                        log.error("状况外异常", e);
                    }
                }
            }
        }

        return false;
    }

    /**
     * 根据 patternSetting 校验字符串
     *
     * @param patternId 正则表达式id，获取对应表达式 p1,p2,p3;pA,pB,pC
     *                       分成：1、[p1,p2,p3]：words作为p1、p2、p3的输入字符
     *                            2、[pA,pB,pC]：同上
     * @param words 输入字符
     * @return String : 1、null\"" 校验通过
     *                  2、校验异常信息
     * @throws RuntimeException 未匹配上
     */
    private String matcher(String patternId, String words, Map<String, String> variablesMap) throws RuntimeException {

        String[] patternIdGroup = patternId.split(";");
        Pattern pattern;

        List<String> messages = new ArrayList<>();
        boolean isMatch = false;
        for (int i = 0; i < patternIdGroup.length; i++) {
            String[] patternIds = patternIdGroup[i].split(",");

            isMatch = true;
            // 串式匹配 上一个匹配上才能匹配下一个
            for (int j = 0; j < patternIds.length; j++) {
                AlarmRegex deviceSCADARegex = this.deviceRegexMap.get(patternIds[j]);
                if (deviceSCADARegex == null) {
                    throw new RuntimeException("未查询到id为:" + patternIds[i] + "的正则表达式");
                }
                // 加载正则前，解析正则表达式，替换变量
                String patternStr = deviceSCADARegex.getPattern();
                if (MapUtils.isNotEmpty(variablesMap)) {
                    for (Map.Entry<String, String> entry : variablesMap.entrySet()) {
                        int index = -1;
                        while((index = patternStr.indexOf("#{" + entry.getKey() + "}")) != -1) {
                            patternStr = patternStr.substring(0, index) + entry.getValue() + patternStr.substring(index + 3 + entry.getKey().length());
                        }
//                        patternStr = patternStr.replace("#{" + entry.getKey() + "}", entry.getValue());
                    }
                }
                pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(words);
                // :号 分割
                String[] notCatchMessage = Optional.ofNullable(deviceSCADARegex.getNotMatchMessage()).orElse(Strings.EMPTY).split(":");
                if (matcher.matches()) {
                    // 存在捕获组
                    int groupCount = matcher.groupCount();
                    if (groupCount > 0) {
                        if (notCatchMessage.length != groupCount) {
                            throw new PatternNotMatchException("警告规范信息(DEVICE_SCADA_INFO_RULE)配置：pattern中捕获组与not_match_message中信息数量不一致");
                        }
                        for (int k = 0; k < groupCount; k++) {
                            if (matcher.group(j) == null) {
                                isMatch = false;
                                // 第一个为未匹配上的信息
                                messages.add(notCatchMessage[k + j > 0 ? 1 : 0]);
                            }
                        }
                    }
                    continue;
                }

                isMatch = j > 0;
                if (j > 0) {
                    messages.add(notCatchMessage[0]);
                }
                break;
            }

            // 匹配上
            if (isMatch) {
                break;
            }
        }

        if (!isMatch) {
            throw new PatternNotMatchException(words + "，未匹配上：" + patternId);
        }

        return StringUtils.join(messages, ",");
    }

    /**
     * 根据 设备列表获取对应的设备信息类型，包含规范信息
     *
     * @param devices 设备列表
     * @return 设备信息类型（含规范信息）
     */
    private List<AlarmTypeRuleModel> searchDeviceInfoType(Collection<Device> devices) {
        Set<String> ids = devices.stream().flatMap(e -> Stream.of(e.getId(), e.getChildId())).collect(Collectors.toSet());
        if (ids.isEmpty()){
            return this.deviceSCADAInfoTypeMapper.selectListAndInfoRuleWhenDeviceEmpty();
        }
        List<AlarmTypeRuleModel> ruleModels = this.deviceSCADAInfoTypeMapper.selectListAndInfoRule(ids);
        return ids.contains(null) ? ListUtils.union(ruleModels, this.deviceSCADAInfoTypeMapper.selectListAndInfoRuleWhenDeviceEmpty()) : ruleModels;
    }

    /**
     * 将Excel中的数据转换为格式化信息
     *
     * @param inputStream 文件输入流
     * @return 格式化输入信息
     */
    private Map<SheetInfo, List<DeviceSCADAInputInfo>> transferExcel(InputStream inputStream) {
        try (
                // 加载 Excel 文件
                Workbook workbook = new XSSFWorkbook(inputStream);
             ) {

            Map<SheetInfo, List<DeviceSCADAInputInfo>> map = new HashMap<>();
            List<DeviceSCADAInputInfo> list;

            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.isSheetHidden(i)) {
                    continue;
                }

                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                if (!sheetName.contains("遥信")) {
                    continue;
                }

                SheetInfo sheetInfo = new SheetInfo();
                sheetInfo.setIndex(i);
                sheetInfo.setName(sheet.getSheetName());

                list = new ArrayList<>();
                // 迭代行
                DeviceSCADAInputInfo deviceSCADAInputInfo;
                int pointCodeIndex = 0, deviceIndex = 0, infoIndex = 0;

                for (Row row : sheet) {
                    // 跳过第一行 attr_oid 位置 告警信号名称
                    if (row.getRowNum() == 1) {
                        for (Cell cell : row) {
                            if (cell.getCellType() != CellType.STRING) {
                                continue;
                            }

                            if ("点号".equals(cell.getStringCellValue().trim())) {
                                pointCodeIndex = cell.getColumnIndex();
                            }

                            if ("间隔".equals(cell.getStringCellValue().trim())) {
                                deviceIndex = cell.getColumnIndex();
                            }

                            if ("信号名称".equals(cell.getStringCellValue().trim())) {
                                infoIndex = cell.getColumnIndex();
                            }
                        }

                        continue;
                    }

                    if (row.getRowNum() <= 1) {
                        continue;
                    }
                    deviceSCADAInputInfo = new DeviceSCADAInputInfo();
                    // 迭代单元格
                    for (Cell cell : row) {
                        int columnIndex = cell.getColumnIndex();
                        if (deviceIndex == columnIndex) {
                            CellType cellType = cell.getCellType();
                            if (cellType == CellType.STRING) {
                                deviceSCADAInputInfo.setDevice(cell.getStringCellValue());
                            } else if (cellType == CellType.NUMERIC) {
                                deviceSCADAInputInfo.setDevice(String.valueOf(cell.getNumericCellValue()));
                            }
                        } else if (infoIndex == columnIndex) {
                            deviceSCADAInputInfo.setWarnInfo(cell.getStringCellValue());
                        } else if (pointCodeIndex == columnIndex) {
                            if (cell.getCellType() == CellType.FORMULA) {
                                CellValue cellValue = formulaEvaluator.evaluate(cell);
                                deviceSCADAInputInfo.setPointCode(String.format("%.0f", cellValue.getNumberValue()));
                            } else {
                                deviceSCADAInputInfo.setPointCode(cell.getStringCellValue());
                            }
                        }
                    }
                    if (StringUtils.isBlank(deviceSCADAInputInfo.getWarnInfo())) {
                        break;
                    }
                    list.add(deviceSCADAInputInfo);
                }

                sheetInfo.setTotalRowNum(list.size());
                map.put(sheetInfo, list);
                break;
            }

            // 关闭文件流
            workbook.close();
            inputStream.close();
            return map;
        } catch (Exception e) {
            log.info("DeviceWarnInfoCheckService#transferExcel: {}", "上传文件数据转换失败");
            throw new RuntimeException(e);
        }
    }

    /**
     * 抽取转换设备信息，包含电压、设备
     *
     * @param infos 输入信息
     * @return 转换后的设备信息
     */
    private List<DeviceSCADAInputInfo> extractDeviceInfo(List<DeviceSCADAInputInfo> infos) {
        ArrayList<DeviceSCADAInputInfo> newInfos = new ArrayList<>(Collections.nCopies(infos.size(), null));
        Collections.copy(newInfos, infos);

        for (DeviceSCADAInputInfo info : newInfos) {
            if (info.getWarnInfo() == null) {
                continue;
            }
            Matcher matcher = pattern.matcher(info.getWarnInfo());
            if (matcher.matches()) {
                String voltage = matcher.group("voltage"), warnInfo = matcher.group("info");
                List<Device> devices = new ArrayList<>();
                // 根据 拼接的正则 捕获设备信息，不够灵活，设备关系无法确定
//                for (RegexPattern regexPattern : regexPatterns) {
//                    Object deviceTypeEnumObj = regexPattern.getDeviceTypeEnum();
//                    if (!(deviceTypeEnumObj instanceof DeviceTypeEnum)) {
//                        continue;
//                    }
//                    DeviceTypeEnum deviceTypeEnum = (DeviceTypeEnum) deviceTypeEnumObj;
//                    String groupName = ((DeviceTypeEnum) deviceTypeEnumObj).getName();
//                    try {
//                        String deviceName = matcher.group(groupName);
//                        if (StringUtils.isNotBlank(deviceName)) {
//                            devices.add(new Device(voltage, deviceName, deviceTypeEnum.getDesc()));
//                        }
//                    } catch (IllegalStateException | IllegalArgumentException e) {
//                        log.debug("未匹配到{}", deviceTypeEnum.getDesc());
//                    }
//                }
                // 去除 电压信息 如果有的话
                try {
                    extractDevice(voltage, info.getWarnInfo().replaceFirst(Optional.ofNullable(voltage).orElse(""), ""), devices, 0);
                } catch (CustomException e) {
                    log.error("告警信息设备解析失败：{}，{} ", warnInfo, e.getMessage());
                }

                info.setVoltage(voltage);
                info.setDevices(devices);
                info.setInfo(warnInfo);

            }

        }

        return newInfos;
    }

    /**
     * 当 {message} 不为空时，拼接 校验信息
     *
     * @param message 校验信息
     * @param info 设备信息
     */
    private void appendTips(String message, DeviceSCADAInputInfo info) {
        if (StringUtils.isNotBlank(message)) {
            List<String> tips = info.getTips();
            tips = tips == null ? new ArrayList<>() : tips;
            tips.add(message);
            info.setTips(tips);
        }
    }

    /**
     * 检查电压
     * @param voltage 电压
     * @return 检验信息
     */
    private String checkVoltage(Device device, DeviceTypeEnum deviceTypeEnum, String voltage, DeviceSCADAInputInfo info, Map<String, String> substationVoltageMap) {
        if (!deviceTypeEnum.isNeedVoltage()) {
            return Strings.EMPTY;
        }
        // 无电压主变 通过
        if (deviceTypeEnum == DeviceTypeEnum.MAIN_TRANSFORMER) {
            if (StringUtils.isBlank(voltage)) {
                return Strings.EMPTY;
            }
            // 主变信号 电压检测
//            Pattern stationPattern = Pattern.compile("^/(.*站)/");
//            Matcher matcher = stationPattern.matcher(info.getAttrOid());
//            if (matcher.find()) {
//                String stationName = matcher.group(1);
//                String stationVoltage = substationVoltageMap.get(stationName);
//                if (!Objects.equals(stationVoltage, voltage)) {
//                    return stationName + "，电压应为" + stationVoltage + "，与信号电压" + voltage + "不符";
//                }
//            }
        }
        String result = Strings.EMPTY;

        // 电容/抗器组 AVC 无电压 默认 66kV
        if (deviceTypeEnum == DeviceTypeEnum.CAPACITANCE_AND_REACTANCE
                && device.getName().contains("AVC")) {
            voltage = device.getName().contains("低压") ? "66kV" : "20kV";
            device.setVoltage(voltage);
        }

        if (StringUtils.isBlank(voltage)) {
            result = "缺少电压等级";
        } else if (voltage.contains("K")){
            result = "电压单位，注意k小写";
        } else if (voltage.contains("v")){
            result = "电压单位，注意V大写";
        }
        return result;
    }

    private void checkDevice(Device device, DeviceSCADAInputInfo info, Map<String, String> substationVoltageMap) {
        if (device == null) {
            return;
        }
        DeviceTypeEnum deviceTypeEnum = DeviceTypeEnum.of(device.getType());
        if (deviceTypeEnum == null) {
            device.setCheckResult(false);
            device.setCheckResultMessage("程序中未配置的设备类型:" + device.getType());
            return;
        }
        // 检查电压
        String voltageCheckResult = checkVoltage(device, deviceTypeEnum, device.getVoltage(), info, substationVoltageMap);
        if (StringUtils.isNotBlank(voltageCheckResult)) {
            device.setCheckResult(false);
            device.setCheckResultMessage(voltageCheckResult);
            return;
        }

        // 380V站用变 改为 380V站用电
        String voltage = device.getVoltage();
//        if ("380V".equals(voltage) && deviceTypeEnum == DeviceTypeEnum.STATION_TRANSFORMER) {
//            device.setType(DeviceTypeEnum.STATION_POWER.getDesc());
//        }

        List<Device> sameTypeDevices = this.deviceMap.get(device.getType());
        if (CollectionUtils.isEmpty(sameTypeDevices)) {
            device.setCheckResult(false);
            device.setCheckResultMessage(checkVoltage(device, deviceTypeEnum, device.getVoltage(), info, substationVoltageMap));
            return;
        }
        String message = null;

        String name  = device.getName();
        List<Device> devices = sameTypeDevices.stream()
                .filter(e -> StringUtils.isBlank(e.getVoltage()) || Objects.equals(e.getVoltage(), voltage))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(devices)) {
            return;
        }
        if (CollectionUtils.isEmpty(devices) && StringUtils.isNotBlank(voltage)) {
            message = "未配置电压等级:" + voltage + "对应的" + deviceTypeEnum.getDesc();
        }
        if (devices.size() > 1) {
            message =  "相同电压等级:" + voltage + "下有多个" + deviceTypeEnum.getDesc() + "设备";
        }

        Device deviceInDB = devices.get(0);

        AlarmRegex deviceSCADARegex = this.deviceRegexMap.get(deviceInDB.getPatternId());

        Pattern pattern = Pattern.compile(deviceSCADARegex.getPattern());
        Matcher matcher = pattern.matcher(name);

        StringBuilder reMessage = new StringBuilder();

        if (matcher.matches()) {
            device.setId(deviceInDB.getId());
            device.setChildId(deviceInDB.getChildId());
            device.setType(deviceInDB.getType());
            device.setPatternId(deviceInDB.getPatternId());
            // 存在捕获组
            if (matcher.find()) {
                String notMatchMessage = Optional.ofNullable(deviceSCADARegex.getNotMatchMessage()).orElse("");
                String[] notMatchMessages = notMatchMessage.split(";");
                for (int i = 0; i < matcher.groupCount(); i++) {
                    String group = matcher.group(i);
                    if (group == null) {
                        if (i > notMatchMessages.length - 1) {
                            reMessage.append("设备：").append(deviceInDB.getId()).append("，pattern配置与未匹配信息数量不一致");
                        }
                        reMessage.append(notMatchMessages[i]);
                    }
                }
            }
        }

        if (StringUtils.isNotBlank(reMessage.toString())) {
            message += reMessage.toString();
        }

        if (StringUtils.isNotBlank(message)) {
            device.setCheckResult(false);
            device.setCheckResultMessage(message);
        }

        device.setCheckResult(true);
    }

    /**
     * 抽取设备
     * @param voltage 电压
     * @param warnInfo 告警信息
     * @param devices  解析后的设备列表
     * @param depth 递归深度 默认递归深度超过10层抛出异常 请传入0
     */
    public void extractDevice(String voltage, String warnInfo, List<Device> devices, int depth) {
        Pattern p;
        Matcher matcher;
        boolean anyMatch = false, isDevice = true;
        for (RegexPattern regexPattern : regexPatterns) {
            if (!(regexPattern.getDeviceTypeEnum() instanceof DeviceTypeEnum)) {
                continue;
            }
            p = Pattern.compile("^(" + regexPattern.getPattern() + ")");
            matcher = p.matcher(warnInfo);
            if (!matcher.find()) {
                continue;
            }
            String catchGroupMessage = matcher.group(0);
            if (StringUtils.isBlank(catchGroupMessage)) {
                continue;
            }

            anyMatch = true;
            isDevice = true;
            for (RegexPattern second : regexPatterns) {
                if (!(second.getDeviceTypeEnum() instanceof DeviceTypeEnum)) {
                    continue;
                }
                if (Objects.equals(second.getPattern(), regexPattern.getPattern())) {
                    continue;
                }
                // 双母线中肯定包含母线
                if (regexPattern.getDeviceTypeEnum() == DeviceTypeEnum.DOUBLE_GENERATRIX &&
                        second.getDeviceTypeEnum() == DeviceTypeEnum.GENERATRIX) {
                    continue;
                }
                p = Pattern.compile("^(" + second.getPattern() + ")");
                matcher = p.matcher(catchGroupMessage);
                if (matcher.find()) {
                    isDevice = false;
                    anyMatch = false;
                    break;
                }
            }

            if (isDevice) {
                warnInfo = warnInfo.replaceFirst(catchGroupMessage, "");
                devices.add(new Device(voltage, catchGroupMessage, ((DeviceTypeEnum)regexPattern.getDeviceTypeEnum()).getDesc()));
            }
        }

        if (anyMatch) {
            if (depth > 10) {
                throw new CustomException("设备解析超出默认递归深度，默认最大递归深度：10");
            }
            extractDevice(voltage, warnInfo, devices, ++depth);
        }

    }

    public void initProgress(AtomicReference<ProgressModel> progressModel, long total) {
        if (progressModel == null) {
            return;
        }
        progressModel.updateAndGet( t -> new ProgressModel(total));
    }

    public void stepProgress(AtomicReference<ProgressModel> progressModel, long step) {
        if (progressModel == null) {
            return;
        }
        progressModel.updateAndGet( t -> {
            t.setProcessed(t.getProcessed() + step);
            return t;
        });
    }

    @Getter
    @Setter
    public static class SheetInfo{

        private Integer index;

        private String name;

        private Integer totalRowNum;

    }

    public void downloadModuleFile(OutputStream outputStream) {
        try {
            ClassPathResource classPathResource = new ClassPathResource("告警信号检测上传文件.xlsx");
//            File file = classPathResource.getFile();
//            FileInputStream fileInputStream = new FileInputStream(file);
//            ResourceUtils.getFile("classpath:告警信号检测上传文件.xlsx");
            InputStream fileInputStream = classPathResource.getInputStream();
            byte[] bytes = new byte[1024];
            while (fileInputStream.read(bytes) != -1) {
                outputStream.write(bytes);
            }
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    public static class RegexPattern{

        private final String pattern;

        private final Object deviceTypeEnum;

        public RegexPattern(String pattern, DeviceTypeEnum deviceTypeEnum) {
            this.pattern = pattern;
            this.deviceTypeEnum = deviceTypeEnum;
        }

        public RegexPattern(String pattern, String desc) {
            this.pattern = pattern;
            this.deviceTypeEnum = desc;
        }

    }

}
