import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Chip, Text, useTheme } from 'react-native-paper';
import { spacing } from '../theme/tokens';

export interface ChoiceOption<T extends string> {
  value: T;
  label: string;
}

interface Props<T extends string> {
  label: string;
  value: T;
  options: ChoiceOption<T>[];
  onChange: (value: T) => void;
}

/** Hàng chip chọn một giá trị (status, priority, khoảng thời gian…). */
export function ChoiceRow<T extends string>({ label, value, options, onChange }: Props<T>) {
  const theme = useTheme();

  return (
    <View style={styles.wrapper}>
      {label ? (
        <Text variant="labelLarge" style={[styles.label, { color: theme.colors.onSurfaceVariant }]}>
          {label}
        </Text>
      ) : null}
      <View style={styles.row}>
        {options.map((option) => {
          const selected = option.value === value;
          return (
            <Chip
              key={option.value}
              selected={selected}
              onPress={() => onChange(option.value)}
              style={[
                styles.chip,
                {
                  backgroundColor: selected ? theme.colors.primaryContainer : theme.colors.surfaceVariant,
                },
              ]}
              textStyle={{ color: selected ? theme.colors.onPrimaryContainer : theme.colors.onSurfaceVariant, fontSize: 12 }}
            >
              {option.label}
            </Chip>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: spacing.md },
  label: { marginBottom: spacing.xs, fontWeight: '600' },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs },
  chip: { borderRadius: 999 },
});
