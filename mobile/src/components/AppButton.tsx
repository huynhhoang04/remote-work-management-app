import React from 'react';
import { StyleSheet } from 'react-native';
import { Button, useTheme } from 'react-native-paper';
import { radius } from '../theme/tokens';

type Variant = 'primary' | 'outline' | 'danger' | 'text';

interface Props {
  label: string;
  onPress: () => void;
  variant?: Variant;
  loading?: boolean;
  disabled?: boolean;
  icon?: string;
  style?: object;
}

/**
 * Nút chuẩn của app — mọi màn hình dùng component này thay vì Button rời.
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function AppButton({ label, onPress, variant = 'primary', loading = false, disabled = false, icon, style }: Props) {
  const theme = useTheme();

  const mode = variant === 'outline' ? 'outlined' : variant === 'text' ? 'text' : 'contained';
  const isDanger = variant === 'danger';

  return (
    <Button
      mode={mode}
      onPress={onPress}
      loading={loading}
      disabled={disabled || loading}
      icon={icon}
      buttonColor={isDanger ? theme.colors.error : undefined}
      textColor={isDanger ? theme.colors.onError : undefined}
      style={[styles.button, style]}
      contentStyle={styles.content}
      labelStyle={styles.label}
    >
      {label}
    </Button>
  );
}

const styles = StyleSheet.create({
  button: { borderRadius: radius.lg },
  content: { minHeight: 52, paddingHorizontal: 8 },
  label: { fontWeight: '600', letterSpacing: 0.3 },
});
