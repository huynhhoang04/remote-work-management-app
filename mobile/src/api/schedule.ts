import type { ScheduleEventDto, ScheduleEventPayload } from '../types/api';
import { api } from './client';

/** API lịch cá nhân — docs/plan-backend/02-danh-sach-api.md §6. */
export const scheduleApi = {
  list(from: string, to: string): Promise<ScheduleEventDto[]> {
    return api
      .get<ScheduleEventDto[]>('/schedule/events', { params: { from, to } })
      .then((response) => response.data);
  },

  create(payload: ScheduleEventPayload): Promise<ScheduleEventDto> {
    return api.post<ScheduleEventDto>('/schedule/events', payload).then((response) => response.data);
  },

  update(eventId: string, payload: Partial<ScheduleEventPayload>): Promise<ScheduleEventDto> {
    return api
      .patch<ScheduleEventDto>(`/schedule/events/${eventId}`, payload)
      .then((response) => response.data);
  },

  remove(eventId: string): Promise<void> {
    return api.delete<void>(`/schedule/events/${eventId}`).then(() => undefined);
  },
};
