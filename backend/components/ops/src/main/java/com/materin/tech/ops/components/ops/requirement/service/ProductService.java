package com.materin.tech.ops.components.ops.requirement.service;

import com.materin.tech.ops.common.exception.BusinessException;
import com.materin.tech.ops.common.exception.ErrorCode;
import com.materin.tech.ops.components.ops.requirement.dto.ProductCreateBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProductMemberBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProductModuleBody;
import com.materin.tech.ops.components.ops.requirement.dto.ProductUpdateBody;
import com.materin.tech.ops.components.ops.requirement.entity.Product;
import com.materin.tech.ops.components.ops.requirement.entity.ProductMember;
import com.materin.tech.ops.components.ops.requirement.entity.ProductModule;
import com.materin.tech.ops.components.ops.requirement.entity.Story;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductMemberMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.ProductModuleMapper;
import com.materin.tech.ops.components.ops.requirement.mapper.StoryMapper;
import com.materin.tech.ops.components.ops.requirement.vo.ProductMemberVo;
import com.materin.tech.ops.components.ops.requirement.vo.ProductModuleVo;
import com.materin.tech.ops.components.ops.requirement.vo.ProductVo;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 产品服务：CRUD / 选项 / 模块树 / 成员授权。
 * <p>列表与详情按数据权限过滤；写操作额外断言单条可见性。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    // ---------- 表列常量（不依赖 APT 生成的 TableDef） ----------
    private static final QueryColumn C_ID = new QueryColumn("id");
    private static final QueryColumn C_NAME = new QueryColumn("name");
    private static final QueryColumn C_CODE = new QueryColumn("code");
    private static final QueryColumn C_STATUS = new QueryColumn("status");
    private static final QueryColumn C_PRODUCT_ID = new QueryColumn("product_id");
    private static final QueryColumn C_PARENT_ID = new QueryColumn("parent_id");
    private static final QueryColumn C_MODULE_ID = new QueryColumn("module_id");
    private static final QueryColumn C_USER_ID = new QueryColumn("user_id");

    private final ProductMapper productMapper;
    private final ProductModuleMapper productModuleMapper;
    private final ProductMemberMapper productMemberMapper;
    private final StoryMapper storyMapper;
    private final ProductDataScopeService dataScopeService;
    private final UserNicknameResolver nicknameResolver;

    // ---------- 产品 ----------

    /** 产品列表（产品量级小，不分页），按可见产品过滤 */
    public List<ProductVo> list(String keyword, Integer status) {
        QueryWrapper qw = QueryWrapper.create();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(C_NAME.like(keyword).or(C_CODE.like(keyword)));
        }
        if (status != null) {
            qw.and(C_STATUS.eq(status));
        }
        Set<Long> visible = dataScopeService.visibleProductIds(dataScopeService.currentUserId());
        if (visible != null) {
            if (visible.isEmpty()) {
                return List.of();
            }
            qw.and(C_ID.in(visible));
        }
        qw.orderBy(C_ID.asc());
        return productMapper.selectListByQuery(qw).stream().map(this::toVo).toList();
    }

    /** 下拉选项：仅启用状态产品 */
    public List<ProductVo> options() {
        return list(null, 1);
    }

    /** 按状态查询 */
    public List<ProductVo> listByStatus(Integer status) {
        return list(null, status);
    }

    /** 产品详情 */
    public ProductVo detail(Long id) {
        Product product = requireVisibleProduct(id);
        return toVo(product);
    }

    /** 新增产品：code 唯一校验；创建人自动成为 owner 成员 */
    @Transactional
    public ProductVo create(ProductCreateBody body) {
        assertCodeUnique(body.getCode(), null);
        Product product = new Product();
        product.setName(body.getName());
        product.setCode(body.getCode());
        product.setDescription(body.getDescription());
        product.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        productMapper.insert(product);
        // 创建人自动授权为产品负责人（APP 通道无用户身份则跳过）
        Long creatorId = dataScopeService.currentUserId();
        if (creatorId != null) {
            ProductMember owner = new ProductMember();
            owner.setProductId(product.getId());
            owner.setUserId(creatorId);
            owner.setRole("owner");
            productMemberMapper.insert(owner);
        }
        return toVo(product);
    }

    /** 编辑产品：code 变更时重新唯一校验 */
    @Transactional
    public void update(Long id, ProductUpdateBody body) {
        Product product = requireVisibleProduct(id);
        if (body.getCode() != null && !body.getCode().equals(product.getCode())) {
            assertCodeUnique(body.getCode(), id);
            product.setCode(body.getCode());
        }
        if (body.getName() != null) product.setName(body.getName());
        if (body.getDescription() != null) product.setDescription(body.getDescription());
        if (body.getStatus() != null) product.setStatus(body.getStatus());
        productMapper.update(product);
    }

    /**
     * 删除产品（逻辑删除）：产品下仍有需求时禁止删除；
     * 同步逻辑删除模块树、物理删除成员授权。
     */
    @Transactional
    public void delete(Long id) {
        requireVisibleProduct(id);
        Long storyCount = storyMapper.selectCountByQuery(
                QueryWrapper.create().where(C_PRODUCT_ID.eq(id)));
        if (storyCount != null && storyCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "产品下仍有需求，无法删除");
        }
        productMapper.deleteById(id);
        productModuleMapper.deleteByQuery(QueryWrapper.create().where(C_PRODUCT_ID.eq(id)));
        productMemberMapper.deleteByQuery(QueryWrapper.create().where(C_PRODUCT_ID.eq(id)));
    }

    // ---------- 产品模块 ----------

    /** 产品模块树 */
    public List<ProductModuleVo> moduleTree(Long productId) {
        requireVisibleProduct(productId);
        List<ProductModule> all = productModuleMapper.selectListByQuery(
                QueryWrapper.create().where(C_PRODUCT_ID.eq(productId)).orderBy(new QueryColumn("sort").asc()));
        Map<Long, List<ProductModule>> byParent = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return buildTree(byParent, 0L);
    }

    private List<ProductModuleVo> buildTree(Map<Long, List<ProductModule>> byParent, Long parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .map(m -> {
                    ProductModuleVo vo = new ProductModuleVo();
                    vo.setId(m.getId());
                    vo.setProductId(m.getProductId());
                    vo.setParentId(m.getParentId());
                    vo.setName(m.getName());
                    vo.setSort(m.getSort());
                    vo.setChildren(buildTree(byParent, m.getId()));
                    return vo;
                })
                .toList();
    }

    /** 新增产品模块 */
    @Transactional
    public void createModule(Long productId, ProductModuleBody body) {
        requireVisibleProduct(productId);
        if (body.getParentId() != null && body.getParentId() > 0) {
            ProductModule parent = productModuleMapper.selectOneById(body.getParentId());
            if (parent == null || !parent.getProductId().equals(productId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父模块不存在或不属于该产品");
            }
        }
        ProductModule module = new ProductModule();
        module.setProductId(productId);
        module.setParentId(body.getParentId() == null ? 0L : body.getParentId());
        module.setName(body.getName());
        module.setSort(body.getSort() == null ? 0 : body.getSort());
        productModuleMapper.insert(module);
    }

    /** 编辑产品模块（改名/排序/挂载点） */
    @Transactional
    public void updateModule(Long moduleId, ProductModuleBody body) {
        ProductModule module = productModuleMapper.selectOneById(moduleId);
        if (module == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模块不存在");
        }
        requireVisibleProduct(module.getProductId());
        if (body.getParentId() != null) {
            if (body.getParentId().equals(moduleId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父模块不能是自己");
            }
            if (body.getParentId() > 0) {
                ProductModule parent = productModuleMapper.selectOneById(body.getParentId());
                if (parent == null || !parent.getProductId().equals(module.getProductId())) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "父模块不存在或不属于该产品");
                }
            }
            module.setParentId(body.getParentId());
        }
        if (body.getName() != null) module.setName(body.getName());
        if (body.getSort() != null) module.setSort(body.getSort());
        productModuleMapper.update(module);
    }

    /** 删除产品模块：存在子模块或被需求引用时禁止 */
    @Transactional
    public void deleteModule(Long moduleId) {
        ProductModule module = productModuleMapper.selectOneById(moduleId);
        if (module == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "模块不存在");
        }
        requireVisibleProduct(module.getProductId());
        Long childCount = productModuleMapper.selectCountByQuery(
                QueryWrapper.create().where(C_PARENT_ID.eq(moduleId)));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模块下存在子模块，无法删除");
        }
        Long storyCount = storyMapper.selectCountByQuery(
                QueryWrapper.create().where(C_MODULE_ID.eq(moduleId)));
        if (storyCount != null && storyCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模块下存在需求，无法删除");
        }
        productModuleMapper.deleteById(moduleId);
    }

    // ---------- 产品成员 ----------

    /** 产品成员列表（含用户昵称） */
    public List<ProductMemberVo> listMembers(Long productId) {
        requireVisibleProduct(productId);
        List<ProductMember> members = productMemberMapper.selectListByQuery(
                QueryWrapper.create().where(C_PRODUCT_ID.eq(productId)));
        Map<Long, String> nicknames = nicknameResolver.resolve(
                members.stream().map(ProductMember::getUserId).toList());
        return members.stream().map(m -> {
            ProductMemberVo vo = new ProductMemberVo();
            vo.setId(m.getId());
            vo.setUserId(m.getUserId());
            vo.setNickname(nicknames.get(m.getUserId()));
            vo.setRole(m.getRole());
            return vo;
        }).toList();
    }

    /** 全量覆盖保存产品成员（先物理删除再批量插入） */
    @Transactional
    public void saveMembers(Long productId, ProductMemberBody body) {
        requireVisibleProduct(productId);
        productMemberMapper.deleteByQuery(QueryWrapper.create().where(C_PRODUCT_ID.eq(productId)));
        List<ProductMember> list = body.getMembers().stream()
                .map(item -> {
                    ProductMember member = new ProductMember();
                    member.setProductId(productId);
                    member.setUserId(item.getUserId());
                    member.setRole(item.getRole() == null || item.getRole().isBlank() ? "member" : item.getRole());
                    return member;
                })
                .toList();
        if (!list.isEmpty()) {
            productMemberMapper.insertBatch(list);
        }
    }

    // ---------- 私有方法 ----------

    /** 取产品并断言可见性，不存在抛 404 */
    private Product requireVisibleProduct(Long id) {
        Product product = productMapper.selectOneById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产品不存在");
        }
        dataScopeService.assertProductVisible(dataScopeService.currentUserId(), id);
        return product;
    }

    /** 产品编码唯一校验（excludeId 用于更新场景排除自身） */
    private void assertCodeUnique(String code, Long excludeId) {
        QueryWrapper qw = QueryWrapper.create().where(C_CODE.eq(code));
        if (excludeId != null) {
            qw.and(C_ID.ne(excludeId));
        }
        Long count = productMapper.selectCountByQuery(qw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "产品编码已存在");
        }
    }

    private ProductVo toVo(Product product) {
        ProductVo vo = new ProductVo();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setCode(product.getCode());
        vo.setDescription(product.getDescription());
        vo.setStatus(product.getStatus());
        return vo;
    }
}
