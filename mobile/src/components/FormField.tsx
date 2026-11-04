import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { HelperText, TextInput, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';

interface Props {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  error?: string;
  helperText?: string;
  secure?: boolean;
  keyboardType?: 'default' | 'email-address' | 'numeric';
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  maxLength?: number;
  multiline?: boolean;
  disabled?: boolean;
  onBlur?: () => void;
  leftIcon?: string;
}

/**
 * Ô nhập chuẩn: icon trái, label, lỗi dưới field, hiện/ẩn mật khẩu.
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function FormField({
  label,
  value,
  onChangeText,
  error,
  helperText,
  secure = false,
  keyboardType = 'default',
  autoCapitalize = 'none',
  maxLength,
  multiline = false,
  disabled = false,
  onBlur,
  leftIcon,
}: Props) {
  const theme = useTheme();
  const [hidden, setHidden] = useState(secure);

  return (
    <View style={styles.wrapper}>
      <TextInput
        mode="outlined"
        label={label}
        value={value}
        onChangeText={onChangeText}
        onBlur={onBlur}
        error={Boolean(error)}
        secureTextEntry={secure ? hidden : false}
        keyboardType={keyboardType}
        autoCapitalize={autoCapitalize}
        maxLength={maxLength}
        multiline={multiline}
        numberOfLines={multiline ? 4 : 1}
        disabled={disabled}
        outlineStyle={styles.outline}
        outlineColor={theme.colors.outline}
        activeOutlineColor={theme.colors.primary}
        style={multiline ? styles.multiline : undefined}
        left={leftIcon ? <TextInput.Icon icon={leftIcon} /> : undefined}
        right={
          secure ? (
            <TextInput.Icon icon={hidden ? 'eye-outline' : 'eye-off-outline'} onPress={() => setHidden((prev) => !prev)} />
          ) : undefined
        }
      />
      {error ? (
        <HelperText type="error" visible style={styles.helper}>
          {error}
        </HelperText>
      ) : helperText ? (
        <HelperText type="info" visible style={styles.helper}>
          {helperText}
        </HelperText>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: spacing.sm },
  outline: { borderRadius: radius.md },
  multiline: { minHeight: 110 },
  helper: { paddingHorizontal: spacing.xs, paddingTop: spacing.xs },
});
