package com.materin.tech.ops.components.ticket.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.PageResult;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ticket.entity.SlaPolicy;
import com.materin.tech.ops.components.ticket.service.SlaPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SLA 策略接口。
 */
@RestController
@RequestMapping("/ticket/slas")
@RequiredArgsConstructor
public class SlaController {

    private final SlaPolicyService slaPolicyService;

    /** 全量列表 */
    @GetMapping
    public Result<List<SlaPolicy>> list() {
        return Result.ok(slaPolicyService.listAll());
    }

    /** 编辑策略 */
    @SaCheckPermission("ticket:sla:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SlaPolicy body) {
        slaPolicyService.update(id, body);
        return Result.ok();
    }
}
