INSERT INTO `user` (`id`, `username`, `password`, `name`, `role`, `phone`) VALUES
(1, 'zhangwei', '123456', '张伟', 'COLLECTION', '13800000001'),
(2, 'liuna', '123456', '刘娜', 'RESTORER', '13800000002'),
(3, 'wangqiang', '123456', '王强', 'BORROWER', '13800000003'),
(4, 'zhaoli', '123456', '赵丽', 'INSURER', '13800000004'),
(5, 'chenming', '123456', '陈明', 'SECURITY', '13800000005'),
(6, 'sunjie', '123456', '孙杰', 'TRANSPORT', '13800000006');

INSERT INTO `artifact` (`id`, `artifact_code`, `name`, `era`, `material`, `dimensions`, `source`, `acquisition_date`, `status`, `description`) VALUES
(1, 'ART-2024-001', '青铜饕餮纹鼎', '商代', '青铜', '高42cm 口径31cm', '殷墟遗址考古发掘', '2024-01-15', 'REGISTERED', '商代晚期青铜礼器，饕餮纹饰精美，保存较为完整'),
(2, 'ART-2024-002', '粉彩花卉纹瓶', '清乾隆', '瓷器', '高45cm 口径12cm', '故宫博物院调拨', '2024-02-20', 'UNDER_RESTORATION', '清代乾隆年间粉彩瓷器，瓶身有裂纹需修复'),
(3, 'ART-2024-003', '敦煌飞天壁画摹本', '唐代', '绢本设色', '120cm×80cm', '敦煌研究院移交', '2024-03-10', 'LOAN_PENDING', '唐代飞天壁画摹本，色彩保存较好'),
(4, 'ART-2024-004', '鎏金铜佛坐像', '北魏', '铜鎏金', '高28cm', '民间捐赠', '2024-04-05', 'ON_LOAN', '北魏时期鎏金铜佛像，造型端庄'),
(5, 'ART-2024-005', '玉镂雕龙纹璧', '西汉', '和田玉', '直径22cm', '徐州汉墓出土', '2024-05-12', 'RETURNED', '西汉玉璧，镂雕工艺精湛');

INSERT INTO `artifact_restriction` (`id`, `artifact_id`, `no_exhibition`, `no_loan`, `min_loan_interval_days`, `max_loan_days`, `loan_conditions`) VALUES
(1, 1, FALSE, FALSE, 30, 90, '需配备恒温恒湿展柜，运输需专业文物运输公司'),
(2, 2, FALSE, TRUE, 0, 0, NULL),
(3, 3, FALSE, FALSE, 60, 120, '需专业包装，展出环境光照不超过50lux'),
(4, 4, FALSE, FALSE, 30, 180, '需单独保险，运输全程GPS监控'),
(5, 5, TRUE, TRUE, 0, 0, NULL);

INSERT INTO `disease` (`id`, `artifact_id`, `disease_type`, `severity`, `description`, `location`, `reported_by`) VALUES
(1, 2, 'CRACK', 'MODERATE', '瓶身中下部有纵向裂纹长约15cm，深度未穿透', '瓶身中下部', 2),
(2, 2, 'DISCOLORATION', 'MILD', '底部釉面有轻微变色发黄现象', '瓶底', 2),
(3, 5, 'SURFACE_WEAR', 'MILD', '玉璧表面有轻微磨损痕迹', '正面中心区域', 1);

INSERT INTO `restoration` (`id`, `artifact_id`, `title`, `plan`, `cleaning_method`, `reinforcement_material`, `status`, `proposed_by`, `approved_by`) VALUES
(1, 2, '粉彩花卉纹瓶裂纹修复', '采用传统金缮工艺修复裂纹，清理表面污渍后进行加固处理', '软毛刷干法清洁', '环氧树脂AB胶+金粉', 'APPROVED', 2, 1),
(2, 5, '玉镂雕龙纹璧表面修复', '对磨损区域进行微抛光处理，恢复表面光泽', '超声波微震清洁', '纳米硅酸盐封护剂', 'COMPLETED', 2, 1);

INSERT INTO `restoration_step` (`id`, `restoration_id`, `step_order`, `action`, `description`, `material_used`, `operator_id`) VALUES
(1, 1, 1, '表面清洁', '使用软毛刷清除瓶身表面灰尘和污渍', '软毛刷、无水乙醇', 2),
(2, 1, 2, '裂纹加固', '沿裂纹注入环氧树脂AB胶进行粘合加固', '环氧树脂AB胶', 2),
(3, 1, 3, '金缮覆盖', '在裂纹加固处覆盖金粉进行金缮美化处理', '金粉+生漆', 2),
(4, 2, 1, '超声波清洁', '使用超声波微震设备清洁表面', '超声波清洁仪', 2),
(5, 2, 2, '微抛光处理', '对磨损区域进行微抛光', '氧化铈抛光粉', 2),
(6, 2, 3, '封护处理', '涂覆纳米硅酸盐封护剂保护表面', '纳米硅酸盐封护剂', 2);

INSERT INTO `loan` (`id`, `artifact_id`, `applicant_id`, `borrowing_institution`, `exhibition_name`, `loan_start_date`, `loan_end_date`, `status`) VALUES
(1, 3, 3, '上海博物馆', '丝路瑰宝——敦煌艺术大展', '2024-07-01', '2024-10-31', 'APPROVED'),
(2, 4, 3, '南京博物院', '金铜佛造像精品展', '2024-08-15', '2025-02-15', 'APPROVED'),
(3, 1, 3, '浙江省博物馆', '青铜文明特展', '2024-09-01', '2024-12-31', 'PENDING');

INSERT INTO `loan_environment` (`id`, `loan_id`, `temperature_min`, `temperature_max`, `humidity_min`, `humidity_max`, `illuminance_max`, `vibration_max`, `security_route`, `setup_date`) VALUES
(1, 1, 18.00, 22.00, 45.00, 55.00, 50.00, 0.0100, '专用通道A->B展厅，全程无台阶', '2024-06-28'),
(2, 2, 16.00, 24.00, 40.00, 60.00, 100.00, 0.0200, '正门->电梯->3楼展厅', '2024-08-10');

INSERT INTO `insurance` (`id`, `loan_id`, `appraised_value`, `deductible`, `deductible_clause`, `transport_liability`, `status`, `verified_by`, `effective_date`, `expiry_date`) VALUES
(1, 1, 5000000.00, 50000.00, '免赔额为保险金额的1%', '运输途中的全部风险由承运方承担', 'ACTIVE', 4, '2024-07-01', '2024-10-31'),
(2, 2, 8000000.00, 80000.00, '免赔额为保险金额的1%', '运输途中的全部风险由承运方承担', 'VERIFIED', 4, '2024-08-15', '2025-02-15');

INSERT INTO `transport` (`id`, `loan_id`, `packaging_standard`, `box_code`, `box_spec`, `inner_material`, `seal_code`, `sealed_by`, `escort_id`, `route`, `status`) VALUES
(1, 1, '一级文物运输标准', 'BOX-2024-001', '120cm×100cm×60cm', '无酸棉+防震泡沫', 'SEAL-001', 5, 6, '北京->上海 专业文物运输车', 'ARRIVED'),
(2, 2, '一级文物运输标准', 'BOX-2024-002', '80cm×60cm×50cm', '无酸棉+防震泡沫', 'SEAL-002', 5, 6, '北京->南京 专业文物运输车', 'IN_TRANSIT');

INSERT INTO `exhibition` (`id`, `loan_id`, `venue`, `showcase_code`, `setup_confirmed_by`, `status`) VALUES
(1, 1, '上海博物馆一楼特展厅', 'SC-001', 5, 'ACTIVE'),
(2, 2, '南京博物院三楼展厅', 'SC-002', NULL, 'PENDING');

INSERT INTO `return_record` (`id`, `loan_id`, `return_transport_id`, `overall_status`, `return_notes`) VALUES
(1, 1, NULL, 'PENDING', '展览尚未结束，待归还');

INSERT INTO `evidence_log` (`artifact_id`, `action`, `from_status`, `to_status`, `operator_id`, `description`, `responsibility_impact`, `related_entity_id`, `related_entity_type`) VALUES
(1, 'CREATE', NULL, 'REGISTERED', 1, '文物登记入库', NULL, 1, 'ARTIFACT'),
(2, 'CREATE', NULL, 'REGISTERED', 1, '文物登记入库', NULL, 2, 'ARTIFACT'),
(2, 'RESTORATION_APPROVED', 'REGISTERED', 'UNDER_RESTORATION', 1, '修复方案审批通过，进入修复流程', NULL, 1, 'RESTORATION'),
(3, 'CREATE', NULL, 'REGISTERED', 1, '文物登记入库', NULL, 3, 'ARTIFACT'),
(3, 'LOAN_APPLY', NULL, 'PENDING', 3, '上海博物馆申请借展', NULL, 1, 'LOAN'),
(3, 'LOAN_APPROVED', 'REGISTERED', 'ON_LOAN', 1, '借展申请审批通过', NULL, 1, 'LOAN'),
(4, 'CREATE', NULL, 'REGISTERED', 1, '文物登记入库', NULL, 4, 'ARTIFACT'),
(4, 'LOAN_APPLY', NULL, 'PENDING', 3, '南京博物院申请借展', NULL, 2, 'LOAN'),
(4, 'LOAN_APPROVED', 'REGISTERED', 'ON_LOAN', 1, '借展申请审批通过', NULL, 2, 'LOAN'),
(5, 'CREATE', NULL, 'REGISTERED', 1, '文物登记入库', NULL, 5, 'ARTIFACT'),
(5, 'RESTORATION_APPROVED', 'REGISTERED', 'UNDER_RESTORATION', 1, '修复方案审批通过', NULL, 2, 'RESTORATION'),
(5, 'RESTORATION_COMPLETED', 'UNDER_RESTORATION', 'REGISTERED', 1, '修复完成', NULL, 2, 'RESTORATION');
