import { requestClient } from '#/api/request';

export interface Ticket {
  assigneeId?: null | number;
  closedAt?: null | string;
  description?: null | string;
  firstResponseAt?: null | string;
  id: number;
  priority: number;
  reporterId: number;
  responseDeadline?: null | string;
  resolvedAt?: null | string;
  resolveDeadline?: null | string;
  slaPolicyId?: null | number;
  status: number;
  ticketNo: string;
  title: string;
  type: number;
}

export interface TicketAction {
  action: number;
  content?: null | string;
  createdAt: string;
  id: number;
  ticketId: number;
  userId?: null | number;
}

export interface SlaPolicy {
  enabled: number;
  firstResponseMinutes: number;
  id: number;
  name: string;
  priority: number;
  resolveMinutes: number;
}

export async function pageTickets(params: Record<string, any>) {
  return requestClient.get<{ list: Ticket[]; total: number }>('/ticket/tickets', { params });
}

export async function ticketDetail(id: number) {
  return requestClient.get<Ticket>(`/ticket/tickets/${id}`);
}

export async function ticketActions(id: number) {
  return requestClient.get<TicketAction[]>(`/ticket/tickets/${id}/actions`);
}

export async function createTicket(data: Partial<Ticket>) {
  return requestClient.post<Ticket>('/ticket/tickets', data);
}

export async function updateTicket(id: number, data: Partial<Ticket>) {
  return requestClient.put(`/ticket/tickets/${id}`, data);
}

export async function delegateTicket(id: number, assigneeId: number) {
  return requestClient.put(`/ticket/tickets/${id}/delegate`, { assigneeId });
}

export async function deleteTicket(id: number) {
  return requestClient.delete(`/ticket/tickets/${id}`);
}

export async function acceptTicket(id: number, assigneeId?: number) {
  return requestClient.put(`/ticket/tickets/${id}/accept`, { assigneeId });
}

export async function reassignTicket(id: number, assigneeId: number) {
  return requestClient.put(`/ticket/tickets/${id}/assignee`, { assigneeId });
}

export async function commentTicket(id: number, content: string) {
  return requestClient.put(`/ticket/tickets/${id}/comment`, { content });
}

export async function changeTicketStatus(id: number, status: number) {
  return requestClient.put(`/ticket/tickets/${id}/status/${status}`);
}

export async function closeTicket(id: number) {
  return requestClient.put(`/ticket/tickets/${id}/close`);
}

export async function listSlas() {
  return requestClient.get<SlaPolicy[]>('/ticket/slas');
}

export async function updateSla(id: number, data: Partial<SlaPolicy>) {
  return requestClient.put(`/ticket/slas/${id}`, data);
}
