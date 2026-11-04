import DateTimePicker, { type DateTimePickerEvent } from '@react-native-community/datetimepicker';
import React, { useEffect, useState } from 'react';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import { Button, HelperText, Modal, Portal, TextInput, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';

interface Props {
  label: string;
  value: Date;
  mode: 'date' | 'time';
  onChange: (value: Date) => void;
  error?: string;
}

function displayValue(value: Date, mode: 'date' | 'time'): string {
  if (mode === 'time') {
    return value.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }
  return value.toLocaleDateString('vi-VN');
}

/** Ô chọn ngày/giờ dùng chung (Android native dialog, iOS modal spinner). */
export function DateTimeField({ label, value, mode, onChange, error }: Props) {
  const theme = useTheme();
  const [visible, setVisible] = useState(false);
  const [draft, setDraft] = useState(value);

  useEffect(() => {
    if (visible) setDraft(value);
  }, [visible, value]);

  const onChangeAndroid = (event: DateTimePickerEvent, date?: Date) => {
    setVisible(false);
    if (event.type === 'set' && date) onChange(date);
  };

  return (
    <View style={styles.wrapper}>
      <Pressable onPress={() => setVisible(true)}>
        <View pointerEvents="none">
      <TextInput
        mode="outlined"
        label={label}
        value={displayValue(value, mode)}
        editable={false}
        error={Boolean(error)}
        outlineStyle={styles.outline}
        outlineColor={theme.colors.outline}
        activeOutlineColor={theme.colors.primary}
        left={<TextInput.Icon icon={mode === 'date' ? 'calendar-outline' : 'clock-outline'} onPress={() => setVisible(true)} />}
        style={styles.input}
      />
        </View>
      </Pressable>
      {error ? (
        <View style={styles.errorRow}>
          <HelperText type="error" visible>
            {error}
          </HelperText>
        </View>
      ) : null}

      {visible && Platform.OS === 'android' ? (
        <DateTimePicker value={value} mode={mode} onChange={onChangeAndroid} />
      ) : null}

      {Platform.OS !== 'android' ? (
        <Portal>
          <Modal visible={visible} onDismiss={() => setVisible(false)} contentContainerStyle={[styles.modal, { backgroundColor: theme.colors.surface }]}>
            <DateTimePicker
              value={draft}
              mode={mode}
              display="spinner"
              onChange={(_event, date) => {
                if (date) setDraft(date);
              }}
            />
            <View style={styles.modalActions}>
              <Button onPress={() => setVisible(false)}>Huỷ</Button>
              <Button
                mode="contained"
                onPress={() => {
                  onChange(draft);
                  setVisible(false);
                }}
              >
                Chọn
              </Button>
            </View>
          </Modal>
        </Portal>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: spacing.sm },
  outline: { borderRadius: radius.md },
  input: { backgroundColor: 'transparent' },
  errorRow: { marginTop: -spacing.xs },
  modal: { marginHorizontal: spacing.lg, borderRadius: radius.xl, padding: spacing.lg },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: spacing.sm, marginTop: spacing.md },
});
