import React, { useEffect, useMemo, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Checkbox, Dialog, Portal, Searchbar, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { spacing } from '../theme/tokens';

export interface SelectOption {
  id: string;
  label: string;
  description?: string;
}

interface Props {
  visible: boolean;
  title: string;
  options: SelectOption[];
  selectedIds: string[];
  onClose: () => void;
  onConfirm: (ids: string[]) => void;
  confirmLabel?: string;
}

/** Dialog chọn nhiều (assignee/nhãn/task liên kết). */
export function MultiSelectDialog({
  visible,
  title,
  options,
  selectedIds,
  onClose,
  onConfirm,
  confirmLabel = 'Xong',
}: Props) {
  const theme = useTheme();
  const [selected, setSelected] = useState<string[]>(selectedIds);
  const [query, setQuery] = useState('');

  useEffect(() => {
    if (visible) {
      setSelected(selectedIds);
      setQuery('');
    }
  }, [visible, selectedIds]);

  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return options;
    return options.filter(
      (option) =>
        option.label.toLowerCase().includes(keyword) ||
        (option.description ?? '').toLowerCase().includes(keyword),
    );
  }, [options, query]);

  const toggle = (id: string) => {
    setSelected((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    );
  };

  return (
    <Portal>
      <Dialog visible={visible} onDismiss={onClose} style={styles.dialog}>
        <Dialog.Title>{title}</Dialog.Title>
        <Dialog.Content style={styles.content}>
          <Searchbar
            placeholder="Tìm…"
            value={query}
            onChangeText={setQuery}
            style={styles.search}
            inputStyle={styles.searchInput}
          />
          <ScrollView style={styles.list} keyboardShouldPersistTaps="handled">
            {filtered.length === 0 ? (
              <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant, padding: spacing.md }}>
                Không có lựa chọn
              </Text>
            ) : (
              filtered.map((option) => {
                const checked = selected.includes(option.id);
                return (
                  <TouchableRipple key={option.id} onPress={() => toggle(option.id)}>
                    <View style={styles.optionRow}>
                      <Checkbox status={checked ? 'checked' : 'unchecked'} />
                      <View style={styles.optionBody}>
                        <Text variant="bodyMedium">{option.label}</Text>
                        {option.description ? (
                          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                            {option.description}
                          </Text>
                        ) : null}
                      </View>
                    </View>
                  </TouchableRipple>
                );
              })
            )}
          </ScrollView>
        </Dialog.Content>
        <Dialog.Actions>
          <Button onPress={onClose}>Huỷ</Button>
          <Button mode="contained" onPress={() => onConfirm(selected)}>
            {confirmLabel}
          </Button>
        </Dialog.Actions>
      </Dialog>
    </Portal>
  );
}

const styles = StyleSheet.create({
  dialog: { maxHeight: '80%' },
  content: { paddingBottom: 0 },
  search: { marginBottom: spacing.sm },
  searchInput: { fontSize: 14 },
  list: { maxHeight: 340 },
  optionRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, paddingVertical: spacing.xs },
  optionBody: { flex: 1 },
});
