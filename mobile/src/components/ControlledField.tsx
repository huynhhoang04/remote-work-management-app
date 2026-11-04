import React from 'react';
import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form';
import { FormField } from './FormField';

interface Props<T extends FieldValues> {
  control: Control<T>;
  name: Path<T>;
  label: string;
  error?: string;
  helperText?: string;
  secure?: boolean;
  keyboardType?: 'default' | 'email-address' | 'numeric';
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  maxLength?: number;
  multiline?: boolean;
  disabled?: boolean;
  leftIcon?: string;
}

/** Ghép react-hook-form Controller với FormField chuẩn của app. */
export function ControlledField<T extends FieldValues>({
  control,
  name,
  label,
  error,
  ...rest
}: Props<T>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field }) => (
        <FormField
          label={label}
          value={field.value ?? ''}
          onChangeText={field.onChange}
          onBlur={field.onBlur}
          error={error}
          {...rest}
        />
      )}
    />
  );
}
