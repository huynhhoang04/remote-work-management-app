import { z } from 'zod';

/** Tin nhắn — backend SendMessageRequest: 1–4000 ký tự. */
export const messageSchema = z.object({
  content: z
    .string()
    .min(1, 'Nội dung tin nhắn không được để trống')
    .max(4000, 'Tin nhắn tối đa 4000 ký tự'),
});

export type MessageForm = z.infer<typeof messageSchema>;
