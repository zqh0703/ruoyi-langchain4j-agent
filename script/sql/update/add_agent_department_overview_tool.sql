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
