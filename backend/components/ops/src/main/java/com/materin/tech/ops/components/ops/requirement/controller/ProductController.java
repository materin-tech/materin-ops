package com.materin.tech.ops.components.ops.requirement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.materin.tech.ops.common.api.Result;
import com.materin.tech.ops.components.ops.requirement.dto.ProductCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProductMemberBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProductModuleBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProductUpdateBody;
import com.materin.tech.ops.components.ops.requirement.service.ProductService;
import com.materin.tech.ops.components.ops.requirement.vo.ProductMemberVo;
import com.materin.tech.ops.components.ops.requirement.vo.ProductModuleVo;
import com.materin.tech.ops.components.ops.requirement.vo.ProductVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品管理接口。查询仅要求登录，写操作按按钮权限鉴权。
 */
@RestController
@RequestMapping("/requirement/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 产品列表（keyword/status 过滤） */
    @GetMapping
    public Result<List<ProductVo>> list(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        return Result.ok(productService.list(keyword, status));
    }

    /** 产品下拉选项（仅启用） */
    @GetMapping("/options")
    public Result<List<ProductVo>> options() {
        return Result.ok(productService.options());
    }

    /** 按状态查询产品 */
    @GetMapping("/status/{status}")
    public Result<List<ProductVo>> listByStatus(@PathVariable Integer status) {
        return Result.ok(productService.listByStatus(status));
    }

    /** 产品详情 */
    @GetMapping("/{id}")
    public Result<ProductVo> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    /** 新增产品 */
    @SaCheckPermission("requirement:product:add")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCreateBody body) {
        return Result.ok(productService.create(body).getId());
    }

    /** 编辑产品 */
    @SaCheckPermission("requirement:product:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateBody body) {
        productService.update(id, body);
        return Result.ok();
    }

    /** 删除产品 */
    @SaCheckPermission("requirement:product:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }

    /** 产品模块树 */
    @GetMapping("/{id}/modules")
    public Result<List<ProductModuleVo>> moduleTree(@PathVariable Long id) {
        return Result.ok(productService.moduleTree(id));
    }

    /** 新增产品模块 */
    @SaCheckPermission("requirement:product:edit")
    @PostMapping("/{id}/modules")
    public Result<Void> createModule(@PathVariable Long id, @Valid @RequestBody ProductModuleBody body) {
        productService.createModule(id, body);
        return Result.ok();
    }

    /** 编辑产品模块 */
    @SaCheckPermission("requirement:product:edit")
    @PutMapping("/modules/{moduleId}")
    public Result<Void> updateModule(@PathVariable Long moduleId, @Valid @RequestBody ProductModuleBody body) {
        productService.updateModule(moduleId, body);
        return Result.ok();
    }

    /** 删除产品模块 */
    @SaCheckPermission("requirement:product:edit")
    @DeleteMapping("/modules/{moduleId}")
    public Result<Void> deleteModule(@PathVariable Long moduleId) {
        productService.deleteModule(moduleId);
        return Result.ok();
    }

    /** 产品成员列表 */
    @GetMapping("/{id}/members")
    public Result<List<ProductMemberVo>> members(@PathVariable Long id) {
        return Result.ok(productService.listMembers(id));
    }

    /** 全量覆盖保存产品成员 */
    @SaCheckPermission("requirement:product:member")
    @PutMapping("/{id}/members")
    public Result<Void> saveMembers(@PathVariable Long id, @Valid @RequestBody ProductMemberBody body) {
        productService.saveMembers(id, body);
        return Result.ok();
    }
}
