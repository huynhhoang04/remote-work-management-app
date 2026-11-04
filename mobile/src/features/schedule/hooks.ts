import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import type { ScheduleEventPayload } from '../../types/api';
import { scheduleApi } from '../../api/schedule';

export function useScheduleEvents(from: string, to: string) {
  return useQuery({
    queryKey: ['schedule', from, to],
    queryFn: () => scheduleApi.list(from, to),
    enabled: Boolean(from && to),
  });
}

/** Lấy một sự kiện theo id (backend không có endpoint đơn) — quét khoảng ±2 năm. */
export function useScheduleEvent(eventId: string) {
  return useQuery({
    queryKey: ['schedule', 'event', eventId],
    queryFn: async () => {
      const from = dayjs().subtract(2, 'year').startOf('year').toISOString();
      const to = dayjs().add(2, 'year').endOf('year').toISOString();
      const events = await scheduleApi.list(from, to);
      return events.find((event) => event.id === eventId) ?? null;
    },
    enabled: Boolean(eventId),
  });
}

function useInvalidateSchedule() {
  const queryClient = useQueryClient();
  return async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['schedule'] }),
      queryClient.invalidateQueries({ queryKey: ['today'] }),
    ]);
  };
}

export function useCreateScheduleEvent() {
  const invalidate = useInvalidateSchedule();
  return useMutation({
    mutationFn: (payload: ScheduleEventPayload) => scheduleApi.create(payload),
    onSuccess: invalidate,
  });
}

export function useUpdateScheduleEvent() {
  const invalidate = useInvalidateSchedule();
  return useMutation({
    mutationFn: ({ eventId, payload }: { eventId: string; payload: Partial<ScheduleEventPayload> }) =>
      scheduleApi.update(eventId, payload),
    onSuccess: invalidate,
  });
}

export function useDeleteScheduleEvent() {
  const invalidate = useInvalidateSchedule();
  return useMutation({
    mutationFn: (eventId: string) => scheduleApi.remove(eventId),
    onSuccess: invalidate,
  });
}
