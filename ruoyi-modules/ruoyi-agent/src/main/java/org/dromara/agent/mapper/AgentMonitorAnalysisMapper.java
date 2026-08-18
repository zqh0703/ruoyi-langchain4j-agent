package org.dromara.agent.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * Fixed aggregate queries used by the read-only Agent monitor tools.
 */
public interface AgentMonitorAnalysisMapper {

    @Select("""
        <script>
        select count(*) as totalCount,
               coalesce(sum(case when status = 0 then 1 else 0 end), 0) as successCount,
               coalesce(sum(case when status = 1 then 1 else 0 end), 0) as failureCount,
               coalesce(avg(cost_time), 0) as averageCostTime,
               coalesce(max(cost_time), 0) as maxCostTime
        from sys_oper_log
        where tenant_id = #{tenantId} and oper_time &gt;= #{startTime}
        <if test="moduleName != null">and title like concat('%', #{moduleName}, '%')</if>
        <if test="operatorName != null">and oper_name like concat('%', #{operatorName}, '%')</if>
        <if test="status != null">and status = #{status}</if>
        </script>
        """)
    OperationSummaryRow selectOperationSummary(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("moduleName") String moduleName,
        @Param("operatorName") String operatorName,
        @Param("status") Integer status
    );

    @Select("""
        <script>
        select coalesce(nullif(title, ''), 'UNKNOWN') as moduleName, count(*) as failureCount
        from sys_oper_log
        where tenant_id = #{tenantId} and oper_time &gt;= #{startTime} and status = 1
        <if test="moduleName != null">and title like concat('%', #{moduleName}, '%')</if>
        <if test="operatorName != null">and oper_name like concat('%', #{operatorName}, '%')</if>
        <if test="status != null">and status = #{status}</if>
        group by coalesce(nullif(title, ''), 'UNKNOWN')
        order by failureCount desc, moduleName asc
        limit 5
        </script>
        """)
    List<OperationModuleFailureRow> selectOperationFailureModules(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("moduleName") String moduleName,
        @Param("operatorName") String operatorName,
        @Param("status") Integer status
    );

    @Select("""
        <script>
        select oper_id as operId, title as moduleName, oper_name as operatorName,
               oper_ip as ipAddress, error_msg as errorMessage, cost_time as costTime,
               oper_time as operationTime
        from sys_oper_log
        where tenant_id = #{tenantId} and oper_time &gt;= #{startTime} and status = 1
        <if test="moduleName != null">and title like concat('%', #{moduleName}, '%')</if>
        <if test="operatorName != null">and oper_name like concat('%', #{operatorName}, '%')</if>
        <if test="status != null">and status = #{status}</if>
        order by oper_time desc, oper_id desc
        limit #{detailLimit}
        </script>
        """)
    List<OperationFailureRow> selectRecentOperationFailures(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("moduleName") String moduleName,
        @Param("operatorName") String operatorName,
        @Param("status") Integer status,
        @Param("detailLimit") int detailLimit
    );

    @Select("""
        <script>
        select count(*) as totalCount,
               coalesce(sum(case when status = '0' then 1 else 0 end), 0) as successCount,
               coalesce(sum(case when status = '1' then 1 else 0 end), 0) as failureCount
        from sys_logininfor
        where tenant_id = #{tenantId} and login_time &gt;= #{startTime}
        <if test="userName != null">and user_name like concat('%', #{userName}, '%')</if>
        <if test="ipAddress != null">and ipaddr = #{ipAddress}</if>
        <if test="status != null">and status = #{status}</if>
        </script>
        """)
    LoginSummaryRow selectLoginSummary(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("userName") String userName,
        @Param("ipAddress") String ipAddress,
        @Param("status") String status
    );

    @Select("""
        <script>
        select coalesce(nullif(user_name, ''), 'UNKNOWN') as item, count(*) as failureCount
        from sys_logininfor
        where tenant_id = #{tenantId} and login_time &gt;= #{startTime} and status = '1'
        <if test="userName != null">and user_name like concat('%', #{userName}, '%')</if>
        <if test="ipAddress != null">and ipaddr = #{ipAddress}</if>
        <if test="status != null">and status = #{status}</if>
        group by coalesce(nullif(user_name, ''), 'UNKNOWN')
        order by failureCount desc, item asc
        limit 5
        </script>
        """)
    List<LoginFailureRankRow> selectLoginFailureUsers(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("userName") String userName,
        @Param("ipAddress") String ipAddress,
        @Param("status") String status
    );

    @Select("""
        <script>
        select coalesce(nullif(ipaddr, ''), 'UNKNOWN') as item, count(*) as failureCount
        from sys_logininfor
        where tenant_id = #{tenantId} and login_time &gt;= #{startTime} and status = '1'
        <if test="userName != null">and user_name like concat('%', #{userName}, '%')</if>
        <if test="ipAddress != null">and ipaddr = #{ipAddress}</if>
        <if test="status != null">and status = #{status}</if>
        group by coalesce(nullif(ipaddr, ''), 'UNKNOWN')
        order by failureCount desc, item asc
        limit 5
        </script>
        """)
    List<LoginFailureRankRow> selectLoginFailureIps(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("userName") String userName,
        @Param("ipAddress") String ipAddress,
        @Param("status") String status
    );

    @Select("""
        <script>
        select coalesce(nullif(device_type, ''), 'UNKNOWN') as deviceType, count(*) as loginCount
        from sys_logininfor
        where tenant_id = #{tenantId} and login_time &gt;= #{startTime}
        <if test="userName != null">and user_name like concat('%', #{userName}, '%')</if>
        <if test="ipAddress != null">and ipaddr = #{ipAddress}</if>
        <if test="status != null">and status = #{status}</if>
        group by coalesce(nullif(device_type, ''), 'UNKNOWN')
        order by loginCount desc, deviceType asc
        limit 5
        </script>
        """)
    List<LoginDeviceRow> selectLoginDevices(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("userName") String userName,
        @Param("ipAddress") String ipAddress,
        @Param("status") String status
    );

    @Select("""
        <script>
        select info_id as infoId, user_name as userName, ipaddr as ipAddress,
               login_location as loginLocation, device_type as deviceType,
               browser, os, msg as message, login_time as loginTime
        from sys_logininfor
        where tenant_id = #{tenantId} and login_time &gt;= #{startTime} and status = '1'
        <if test="userName != null">and user_name like concat('%', #{userName}, '%')</if>
        <if test="ipAddress != null">and ipaddr = #{ipAddress}</if>
        <if test="status != null">and status = #{status}</if>
        order by login_time desc, info_id desc
        limit #{detailLimit}
        </script>
        """)
    List<LoginFailureRow> selectRecentLoginFailures(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("userName") String userName,
        @Param("ipAddress") String ipAddress,
        @Param("status") String status,
        @Param("detailLimit") int detailLimit
    );

    @Data
    class OperationSummaryRow {
        private long totalCount;
        private long successCount;
        private long failureCount;
        private Double averageCostTime;
        private Long maxCostTime;
    }

    @Data
    class OperationModuleFailureRow {
        private String moduleName;
        private long failureCount;
    }

    @Data
    class OperationFailureRow {
        private Long operId;
        private String moduleName;
        private String operatorName;
        private String ipAddress;
        private String errorMessage;
        private Long costTime;
        private Date operationTime;
    }

    @Data
    class LoginSummaryRow {
        private long totalCount;
        private long successCount;
        private long failureCount;
    }

    @Data
    class LoginFailureRankRow {
        private String item;
        private long failureCount;
    }

    @Data
    class LoginDeviceRow {
        private String deviceType;
        private long loginCount;
    }

    @Data
    class LoginFailureRow {
        private Long infoId;
        private String userName;
        private String ipAddress;
        private String loginLocation;
        private String deviceType;
        private String browser;
        private String os;
        private String message;
        private Date loginTime;
    }

}
