-- Add a durable execution status to persisted Agent tool messages.
set @column_exists = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'agent_message'
      and column_name = 'tool_status'
);

set @ddl = if(
    @column_exists = 0,
    'alter table agent_message add column tool_status varchar(20) default null comment ''Tool execution status'' after tool_result',
    'select 1'
);

prepare statement from @ddl;
execute statement;
deallocate prepare statement;

update agent_message
set tool_status = case
    when content like 'Tool failed%' then 'FAILED'
    else 'SUCCESS'
end
where role = 'tool'
  and tool_status is null;
