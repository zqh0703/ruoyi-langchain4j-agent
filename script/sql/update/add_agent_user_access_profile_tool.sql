-- Register the read-only user access profile tool for an existing Agent database.
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
