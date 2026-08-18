-- Two-phase Agent write actions. Safe to run once on an existing Agent database.
create table if not exists agent_action_request (
    id                bigint(20)    not null                    comment 'Action request ID',
    tenant_id         varchar(20)   default '000000'            comment 'Tenant ID',
    session_id        bigint(20)    not null                    comment 'Conversation session ID',
    agent_id          bigint(20)    not null                    comment 'Agent ID',
    run_log_id        bigint(20)    not null                    comment 'Run log that proposed the action',
    tool_message_id   bigint(20)    default null                comment 'Related tool message ID',
    tool_code         varchar(100)  not null                    comment 'Write tool code',
    risk_level        varchar(20)   not null                    comment 'MEDIUM or HIGH',
    status            varchar(32)   not null                    comment 'Action state',
    request_key       varchar(64)   not null                    comment 'Idempotency hash',
    arguments_json    text          not null                    comment 'Sanitized normalized arguments',
    preview_json      longtext      not null                    comment 'Sanitized preview and stale-state snapshot',
    summary           varchar(1000) not null                    comment 'Human-readable action summary',
    result_json       longtext      default null                comment 'Sanitized execution result',
    error_code        varchar(64)   default null                comment 'Failure code',
    error_message     varchar(2000) default null                comment 'Failure detail',
    expires_at        datetime      not null                    comment 'Confirmation expiry time',
    confirmed_by      bigint(20)    default null                comment 'Confirming user ID',
    confirmed_time    datetime      default null                comment 'Confirmation time',
    started_time      datetime      default null                comment 'Execution start time',
    finished_time     datetime      default null                comment 'Execution finish time',
    duration_ms       bigint(20)    default null                comment 'Execution duration',
    version           int(11)       default 0 not null          comment 'Optimistic lock version',
    create_dept       bigint(20)    default null                comment 'Creator department',
    create_by         bigint(20)    default null                comment 'Creator user ID',
    create_time       datetime      default null                comment 'Create time',
    update_by         bigint(20)    default null                comment 'Updater user ID',
    update_time       datetime      default null                comment 'Update time',
    primary key (id),
    unique key uk_agent_action_request (tenant_id, request_key),
    key idx_agent_action_session (session_id, create_by, create_time),
    key idx_agent_action_status (status, expires_at),
    key idx_agent_action_run (run_log_id)
) engine=innodb comment='Two-phase Agent action request';

set @action_column_exists = (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'agent_message' and column_name = 'action_request_id'
);
set @action_column_sql = if(
    @action_column_exists = 0,
    'alter table agent_message add column action_request_id bigint(20) default null comment ''Related action request ID'' after run_log_id',
    'select 1'
);
prepare action_column_stmt from @action_column_sql;
execute action_column_stmt;
deallocate prepare action_column_stmt;

set @action_index_exists = (
    select count(*) from information_schema.statistics
    where table_schema = database() and table_name = 'agent_message' and index_name = 'idx_agent_message_action'
);
set @action_index_sql = if(
    @action_index_exists = 0,
    'alter table agent_message add key idx_agent_message_action (action_request_id)',
    'select 1'
);
prepare action_index_stmt from @action_index_sql;
execute action_index_stmt;
deallocate prepare action_index_stmt;

insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values
    (9, '000000', 'system_user_create', 'System user create',
     'Prepare a normal system user with exact department and roles. Requires explicit UI confirmation.',
     'system-write', 'MEDIUM', '1', '0', 1, sysdate(), 'Two-phase write tool'),
    (10, '000000', 'system_user_status_change', 'System user status change',
     'Prepare enabling or disabling one exact user account with a reason. Requires explicit UI confirmation.',
     'system-write', 'HIGH', '1', '0', 1, sysdate(), 'Two-phase write tool'),
    (11, '000000', 'system_user_role_assign', 'System user role assignment',
     'Prepare incremental role addition or removal for one exact user. Requires explicit UI confirmation.',
     'system-write', 'HIGH', '1', '0', 1, sysdate(), 'Two-phase write tool'),
    (12, '000000', 'system_department_create', 'System department create',
     'Prepare a department under one exact parent with an optional leader. Requires explicit UI confirmation.',
     'system-write', 'MEDIUM', '1', '0', 1, sysdate(), 'Two-phase write tool')
on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    category = values(category),
    risk_level = values(risk_level),
    require_confirmation = values(require_confirmation),
    status = values(status),
    update_time = sysdate();

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values
    (9, '000000', 1, 9, '1', 1, sysdate(), 'Enable system_user_create for the demo Agent'),
    (10, '000000', 1, 10, '1', 1, sysdate(), 'Enable system_user_status_change for the demo Agent'),
    (11, '000000', 1, 11, '1', 1, sysdate(), 'Enable system_user_role_assign for the demo Agent'),
    (12, '000000', 1, 12, '1', 1, sysdate(), 'Enable system_department_create for the demo Agent')
on duplicate key update enabled = values(enabled), update_time = sysdate();

insert ignore into sys_menu values
    ('2010', 'Agent action list', '2002', '2', '#', '', '', 1, 0, 'F', '0', '0', 'agent:action:list', '#', 103, 1, sysdate(), null, null, ''),
    ('2011', 'Agent action confirm', '2002', '3', '#', '', '', 1, 0, 'F', '0', '0', 'agent:action:confirm', '#', 103, 1, sysdate(), null, null, ''),
    ('2012', 'Agent action cancel', '2002', '4', '#', '', '', 1, 0, 'F', '0', '0', 'agent:action:cancel', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu values ('3', '2010'), ('3', '2011'), ('3', '2012');
