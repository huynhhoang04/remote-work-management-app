import React from 'react';
import { StyleSheet } from 'react-native';
import { Chip } from 'react-native-paper';
import { roleColors } from '../theme/tokens';
import type { ProjectRole } from '../types/api';
import { STR } from '../utils/constants';

interface Props {
  role: ProjectRole | string;
}

/**
 * Chip hiển thị vai trò trong dự án (màu theo role).
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function RoleChip({ role }: Props) {
  const color = roleColors[role] ?? '#546E7A';
  const label = (STR.roles as Record<string, string>)[role] ?? role;

  return (
    <Chip compact textStyle={[styles.text, { color }]} style={[styles.chip, { backgroundColor: `${color}1A` }]}>
      {label}
    </Chip>
  );
}

const styles = StyleSheet.create({
  chip: { alignSelf: 'flex-start' },
  text: { fontSize: 11, fontWeight: '600' },
});
