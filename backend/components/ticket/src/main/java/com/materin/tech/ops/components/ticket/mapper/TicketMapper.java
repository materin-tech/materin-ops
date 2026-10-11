package com.materin.tech.ops.components.ticket.mapper;

import com.materin.tech.ops.components.ticket.entity.Ticket;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Ticket Mapper。
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 当日最大工单号（原生 SQL，绕过逻辑删除过滤，避免流水号与已删工单冲突）。
     */
    @Select("SELECT MAX(ticket_no) FROM ticket WHERE ticket_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxTicketNo(@Param("prefix") String prefix);
}
