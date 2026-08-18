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
