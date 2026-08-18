-- Register role discovery so the Agent can present valid choices before write actions.
insert into agent_tool_definition (
    id, tenant_id, tool_code, tool_name, description, category, risk_level,
    require_confirmation, status, create_by, create_time, remark
) values (
    13, '000000', 'system_role_search', 'System role search',
    'Search role names and role keys, or list visible roles when no keyword is supplied.',
    'system', 'LOW', '0', '0', 1, sysdate(), 'Read-only role discovery tool'
) on duplicate key update
    tool_name = values(tool_name),
    description = values(description),
    category = values(category),
    risk_level = values(risk_level),
    require_confirmation = values(require_confirmation),
    status = values(status),
    update_time = sysdate();

insert into agent_config_tool (
    id, tenant_id, agent_id, tool_id, enabled, create_by, create_time, remark
) values (
    13, '000000', 1, 13, '1', 1, sysdate(), 'Enable system_role_search for the demo Agent'
) on duplicate key update
    enabled = values(enabled),
    update_time = sysdate();
