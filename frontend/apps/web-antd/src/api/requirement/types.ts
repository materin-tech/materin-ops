/** 产品 */
export interface Product {
  code: string;
  description?: null | string;
  id: number;
  name: string;
  status: number;
}

/** 产品模块树节点 */
export interface ProductModuleNode {
  children: ProductModuleNode[];
  id: number;
  name: string;
  parentId: number;
  productId: number;
  sort: number;
}

/** 产品成员 */
export interface ProductMember {
  id?: number;
  nickname?: null | string;
  role: string;
  userId: number;
}

/** 需求 */
export interface Story {
  description?: null | string;
  id: number;
  moduleId?: null | number;
  moduleTitle?: null | string;
  ownerId?: null | number;
  ownerName?: null | string;
  priority: number;
  productId: number;
  productName?: null | string;
  sort: number;
  status: number;
  title: string;
}

/** 项目 */
export interface Project {
  code: string;
  description?: null | string;
  endDate?: null | string;
  id: number;
  name: string;
  productIds?: number[];
  productNames?: string[];
  startDate?: null | string;
  status: number;
}

/** 迭代 */
export interface Iteration {
  actualEnd?: null | string;
  actualStart?: null | string;
  id: number;
  name: string;
  planEnd?: null | string;
  planStart?: null | string;
  projectId: number;
  projectName?: null | string;
  status: number;
}

/** 任务 */
export interface Task {
  actualHours?: null | number;
  assigneeId?: null | number;
  assigneeName?: null | string;
  description?: null | string;
  id: number;
  iterationId?: null | number;
  iterationName?: null | string;
  planEnd?: null | string;
  planHours?: null | number;
  planStart?: null | string;
  priority: number;
  projectId: number;
  projectName?: null | string;
  status: number;
  storyId?: null | number;
  storyTitle?: null | string;
  title: string;
  type: number;
}

/** 分页响应 */
export interface PageResult<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}
