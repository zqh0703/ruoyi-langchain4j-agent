-- ----------------------------
-- Agent module tables
-- ----------------------------
create table if not exists agent_config (
    id              bigint(20)      not null                   comment 'Agent ID',
    tenant_id       varchar(20)     default '000000'           comment '租户编号',
    agent_name      varchar(100)    not null                   comment 'Agent名称',
    agent_code      varchar(100)    not null                   comment 'Agent编码',
    provider        varchar(50)     default 'deepseek'         comment '模型供应商',
    model_name      varchar(100)    default 'deepseek-v4-pro'  comment '模型名称',
    system_prompt   text                                       comment '系统提示词',
    temperature     decimal(3,2)    default 0.70               comment '温度参数',
    max_tokens      int(11)         default 2048               comment '最大输出Token',
    enable_tool     char(1)         default '1'                comment '是否启用工具（0否 1是）',
    status          char(1)         default '0'                comment '状态（0正常 1停用）',
    create_dept     bigint(20)      default null               comment '创建部门',
    create_by       bigint(20)      default null               comment '创建者',
    create_time     datetime                                   comment '创建时间',
    update_by       bigint(20)      default null               comment '更新者',
    update_time     datetime                                   comment '更新时间',
    remark          varchar(500)    default ''                 comment '备注',
    primary key (id),
    unique key uk_agent_config_code (tenant_id, agent_code),
    key idx_agent_config_status (status)
) engine=innodb comment = 'Agent配置表';

create table if not exists agent_tool_definition (
    id                    bigint(20)      not null                   comment 'Tool ID',
    tenant_id             varchar(20)     default '000000'           comment 'Tenant ID',
    tool_code             varchar(100)    not null                   comment 'Stable tool code',
    tool_name             varchar(100)    not null                   comment 'Display name',
    description           varchar(500)    default ''                 comment 'Description for the model',
    category              varchar(50)     default 'system'           comment 'Tool category',
    risk_level            varchar(20)     default 'LOW'              comment 'LOW MEDIUM HIGH',
    require_confirmation  char(1)         default '0'                comment 'Require confirmation',
    status                char(1)         default '0'                comment '0 enabled 1 disabled',
    create_dept           bigint(20)      default null               comment 'Create department',
    create_by             bigint(20)      default null               comment 'Creator',
    create_time           datetime                                   comment 'Create time',
    update_by             bigint(20)      default null               comment 'Updater',
    update_time           datetime                                   comment 'Update time',
    remark                varchar(500)    default null               comment 'Remark',
    primary key (id),
    unique key uk_agent_tool_code (tenant_id, tool_code),
    key idx_agent_tool_status (status)
) engine=innodb comment = 'Agent tool definition';

create table if not exists agent_config_tool (
    id             bigint(20)      not null                   comment 'Relation ID',
    tenant_id      varchar(20)     default '000000'           comment 'Tenant ID',
    agent_id       bigint(20)      not null                   comment 'Agent ID',
    tool_id        bigint(20)      not null                   comment 'Tool ID',
    enabled        char(1)         default '1'                comment '1 enabled 0 disabled',
    config_json    text                                       comment 'Per-Agent tool configuration',
    create_dept    bigint(20)      default null               comment 'Create department',
    create_by      bigint(20)      default null               comment 'Creator',
    create_time    datetime                                   comment 'Create time',
    update_by      bigint(20)      default null               comment 'Updater',
    update_time    datetime                                   comment 'Update time',
    remark         varchar(500)    default null               comment 'Remark',
    primary key (id),
    unique key uk_agent_config_tool (tenant_id, agent_id, tool_id),
    key idx_agent_config_tool_enabled (agent_id, enabled)
) engine=innodb comment = 'Agent tool allowlist';

insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values (
    1, '000000', 'system_user_count', 'System user count',
    'Count active, non-deleted users in the current tenant. Use this tool instead of guessing the count.',
    'system', 'LOW', '0', '0', 1, sysdate(), 'First read-only Agent business tool'
) on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    status = values(status);

create table if not exists agent_session (
    id                 bigint(20)      not null                   comment '会话ID',
    tenant_id          varchar(20)     default '000000'           comment '租户编号',
    agent_id           bigint(20)      not null                   comment 'Agent ID',
    title              varchar(200)    default ''                 comment '会话标题',
    status             char(1)         default '0'                comment '状态（0正常 1归档）',
    last_message_time  datetime                                   comment '最后消息时间',
    create_dept        bigint(20)      default null               comment '创建部门',
    create_by          bigint(20)      default null               comment '创建者',
    create_time        datetime                                   comment '创建时间',
    update_by          bigint(20)      default null               comment '更新者',
    update_time        datetime                                   comment '更新时间',
    remark             varchar(500)    default ''                 comment '备注',
    primary key (id),
    key idx_agent_session_agent (agent_id),
    key idx_agent_session_time (last_message_time)
) engine=innodb comment = 'Agent会话表';

create table if not exists agent_message (
    id                 bigint(20)      not null                   comment '消息ID',
    tenant_id          varchar(20)     default '000000'           comment '租户编号',
    session_id         bigint(20)      not null                   comment '会话ID',
    agent_id           bigint(20)      not null                   comment 'Agent ID',
    run_log_id         bigint(20)      default null               comment 'Run log ID',
    role               varchar(20)     not null                   comment '消息角色',
    content            longtext                                   comment '消息内容',
    tool_name          varchar(100)    default null               comment '工具名称',
    tool_args          text                                       comment '工具参数',
    tool_result        longtext                                   comment '工具结果',
    tool_status        varchar(20)    default null               comment 'Tool execution status',
    tool_duration_ms   bigint(20)      default null               comment 'Tool duration in milliseconds',
    prompt_tokens      int(11)         default 0                  comment '提示词Token数',
    completion_tokens  int(11)         default 0                  comment '输出Token数',
    seq                int(11)         default 0                  comment '消息序号',
    create_dept        bigint(20)      default null               comment '创建部门',
    create_by          bigint(20)      default null               comment '创建者',
    create_time        datetime                                   comment '创建时间',
    update_by          bigint(20)      default null               comment '更新者',
    update_time        datetime                                   comment '更新时间',
    primary key (id),
    key idx_agent_message_session (session_id, seq),
    key idx_agent_message_agent (agent_id),
    key idx_agent_message_run_log (run_log_id)
) engine=innodb comment = 'Agent消息表';

create table if not exists agent_run_log (
    id             bigint(20)      not null                   comment '执行日志ID',
    tenant_id      varchar(20)     default '000000'           comment '租户编号',
    agent_id       bigint(20)      default null               comment 'Agent ID',
    session_id     bigint(20)      default null               comment '会话ID',
    provider       varchar(50)     default ''                 comment '模型供应商',
    model_name     varchar(100)    default ''                 comment '模型名称',
    request_body   longtext                                   comment '请求内容',
    response_body  longtext                                   comment '响应内容',
    status         char(1)         default '0'                comment '状态（0成功 1失败）',
    error_msg      text                                       comment '错误信息',
    duration_ms    bigint(20)      default 0                  comment '耗时毫秒',
    create_dept    bigint(20)      default null               comment '创建部门',
    create_by      bigint(20)      default null               comment '创建者',
    create_time    datetime                                   comment '创建时间',
    update_by      bigint(20)      default null               comment '更新者',
    update_time    datetime                                   comment '更新时间',
    primary key (id),
    key idx_agent_run_log_agent (agent_id),
    key idx_agent_run_log_session (session_id),
    key idx_agent_run_log_status (status),
    key idx_agent_run_log_time (create_time)
) engine=innodb comment = 'Agent执行日志表';

-- ----------------------------
-- Agent module menu
-- ----------------------------
insert into sys_menu values('2000', '智能体中心', '0', '6', 'agent', null, '', 1, 0, 'M', '0', '0', '', 'chat-dot-round', 103, 1, sysdate(), null, null, '智能体中心目录');
insert into sys_menu values('2001', 'Agent配置', '2000', '1', 'config', 'agent/config/index', '', 1, 0, 'C', '0', '0', 'agent:config:list', 'setting', 103, 1, sysdate(), null, null, 'Agent配置菜单');
insert into sys_menu values('2002', 'Agent调试', '2000', '2', 'chat', 'agent/chat/index', '', 1, 0, 'C', '0', '0', 'agent:chat:list', 'message', 103, 1, sysdate(), null, null, 'Agent调试菜单');
insert into sys_menu values('2003', '执行记录', '2000', '3', 'run', 'agent/run/index', '', 1, 0, 'C', '0', '0', 'agent:run:list', 'list', 103, 1, sysdate(), null, null, 'Agent执行记录菜单');

insert into sys_menu values('2004', 'Agent配置查询', '2001', '1', '#', '', '', 1, 0, 'F', '0', '0', 'agent:config:query', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2005', 'Agent配置新增', '2001', '2', '#', '', '', 1, 0, 'F', '0', '0', 'agent:config:add', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2006', 'Agent配置修改', '2001', '3', '#', '', '', 1, 0, 'F', '0', '0', 'agent:config:edit', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2007', 'Agent配置删除', '2001', '4', '#', '', '', 1, 0, 'F', '0', '0', 'agent:config:remove', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2008', 'Agent消息发送', '2002', '1', '#', '', '', 1, 0, 'F', '0', '0', 'agent:chat:send', '#', 103, 1, sysdate(), null, null, '');
insert into sys_menu values('2009', '执行记录查询', '2003', '1', '#', '', '', 1, 0, 'F', '0', '0', 'agent:run:query', '#', 103, 1, sysdate(), null, null, '');

-- Give the built-in department-scope role access to the new demo module.
insert into sys_role_menu values ('3', '2000');
insert into sys_role_menu values ('3', '2001');
insert into sys_role_menu values ('3', '2002');
insert into sys_role_menu values ('3', '2003');
insert into sys_role_menu values ('3', '2004');
insert into sys_role_menu values ('3', '2005');
insert into sys_role_menu values ('3', '2006');
insert into sys_role_menu values ('3', '2007');
insert into sys_role_menu values ('3', '2008');
insert into sys_role_menu values ('3', '2009');

-- Default Agent for local demonstration.
insert into agent_config values(
    1,
    '000000',
    'RuoYi项目助手',
    'resume_agent',
    'deepseek',
    'deepseek-v4-pro',
    '你是一个后台管理系统中的智能助手，擅长结合系统数据回答问题，并输出结构清晰、可执行的建议。',
    0.70,
    2048,
    '1',
    '0',
    103,
    1,
    sysdate(),
    null,
    null,
    '默认演示Agent'
);

-- Keep the demonstration Agent aligned with the basic chat milestone.
update agent_config set enable_tool = '1', system_prompt = 'Focus on helping the user develop and explain this RuoYi-Vue-Plus Agent module. Give step-by-step guidance suitable for a developer who knows MySQL and is learning LangChain4j. When a project fact is not available in the conversation, state the uncertainty instead of guessing.' where id = 1;

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values (
    1, '000000', 1, 1, '1', 1, sysdate(), 'Enable system_user_count for the demo Agent'
) on duplicate key update
    enabled = values(enabled),
    update_time = sysdate();

insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values (
    2, '000000', 'system_user_search', 'System user search',
    'Search visible system users by keyword, department, role, and account status with structured results.',
    'system', 'LOW', '0', '0', 1, sysdate(), 'Read-only parameterized user search tool'
) on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    status = values(status);

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values (
    2, '000000', 1, 2, '1', 1, sysdate(), 'Enable system_user_search for the demo Agent'
) on duplicate key update
    enabled = values(enabled),
    update_time = sysdate();
insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values (
    3, '000000', 'system_user_access_profile', 'System user access profile',
    'Get one visible user department, posts, roles, account status, and effective menu permissions.',
    'system', 'LOW', '0', '0', 1, sysdate(), 'Read-only user access profile tool'
) on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    status = values(status);

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values (
    3, '000000', 1, 3, '1', 1, sysdate(), 'Enable system_user_access_profile for the demo Agent'
) on duplicate key update
    enabled = values(enabled),
    update_time = sysdate();

-- Register the read-only department overview tool for an existing Agent database.
insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values (
    4, '000000', 'system_department_overview', 'System department overview',
    'Get one department parent, leader, direct children, user counts, and post count.',
    'system', 'LOW', '0', '0', 1, sysdate(), 'Read-only department overview tool'
) on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    status = values(status);

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values (
    4, '000000', 1, 4, '1', 1, sysdate(), 'Enable system_department_overview for the demo Agent'
) on duplicate key update
    enabled = values(enabled),
    update_time = sysdate();

-- Register the remaining read-only enterprise administration Agent tools.
insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values
    (5, '000000', 'system_role_overview', 'System role overview',
     'Get one role status, data scope, member count, optional members, and menu permissions.',
     'system', 'LOW', '0', '0', 1, sysdate(), 'Read-only role overview tool'),
    (6, '000000', 'system_permission_diagnosis', 'System permission diagnosis',
     'Explain whether a user has a menu permission, its source roles, blocking reasons, and candidate roles.',
     'system', 'LOW', '0', '0', 1, sysdate(), 'Read-only permission diagnosis tool'),
    (7, '000000', 'monitor_operation_analysis', 'Monitor operation analysis',
     'Analyze operation success, failures, duration, failure modules, and recent errors for up to 30 days.',
     'monitor', 'LOW', '0', '0', 1, sysdate(), 'Read-only operation log analysis tool'),
    (8, '000000', 'monitor_login_risk_analysis', 'Monitor login risk analysis',
     'Analyze login failures, account and IP rankings, device distribution, and recent failures for up to 30 days.',
     'monitor', 'LOW', '0', '0', 1, sysdate(), 'Read-only login risk analysis tool')
on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    category = values(category),
    status = values(status);

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values
    (5, '000000', 1, 5, '1', 1, sysdate(), 'Enable system_role_overview for the demo Agent'),
    (6, '000000', 1, 6, '1', 1, sysdate(), 'Enable system_permission_diagnosis for the demo Agent'),
    (7, '000000', 1, 7, '1', 1, sysdate(), 'Enable monitor_operation_analysis for the demo Agent'),
    (8, '000000', 1, 8, '1', 1, sysdate(), 'Enable monitor_login_risk_analysis for the demo Agent')
on duplicate key update
    enabled = values(enabled),
    update_time = sysdate();

-- The seven core tools replace the original count-only demonstration tool.
update agent_tool_definition
set status = '1', update_time = sysdate()
where tool_code = 'system_user_count';

update agent_config_tool relation_table
join agent_tool_definition definition on definition.id = relation_table.tool_id
set relation_table.enabled = '0', relation_table.update_time = sysdate()
where definition.tool_code = 'system_user_count';
