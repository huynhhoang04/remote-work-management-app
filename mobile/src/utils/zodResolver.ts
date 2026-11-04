import type { FieldValues, Resolver } from 'react-hook-form';

/** Cấu trúc tối thiểu của kết quả safeParse (không phụ thuộc phiên bản zod). */
interface ParseIssue {
  path?: ReadonlyArray<PropertyKey>;
  message?: string;
}

interface ParseResult<T> {
  success: boolean;
  data?: T;
  error?: { issues?: ReadonlyArray<ParseIssue> };
}

/**
 * Resolver zod gọn cho react-hook-form (thay @hookform/resolvers để tránh xung đột peer deps).
 * @reference docs/plan-app/01-phase-1.md §7
 */
export function zodResolver<T extends FieldValues>(schema: {
  safeParse: (value: unknown) => unknown;
}): Resolver<T> {
  return async (values) => {
    const result = schema.safeParse(values) as ParseResult<T>;

    if (result.success && result.data !== undefined) {
      return { values: result.data, errors: {} };
    }

    const errors: Record<string, { type: string; message: string }> = {};
    for (const issue of result.error?.issues ?? []) {
      const key = String(issue.path?.[0] ?? 'root');
      if (!errors[key]) {
        errors[key] = { type: 'validation', message: issue.message ?? 'Giá trị không hợp lệ' };
      }
    }

    return { values: {}, errors: errors as never };
  };
}
