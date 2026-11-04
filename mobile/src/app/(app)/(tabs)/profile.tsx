import * as ImagePicker from 'expo-image-picker';
import { router } from 'expo-router';
import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Alert, KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { Avatar, Card, Icon, List, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { normalizeError } from '@/api/errors';
import { authImageSource } from '@/api/files';
import { AppButton } from '@/components/AppButton';
import { ControlledField } from '@/components/ControlledField';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { TabHeader } from '@/components/TabHeader';
import { useChangePassword, useLogout } from '@/features/auth/hooks';
import { useMe, useUpdateMe, useUploadAvatar } from '@/features/profile/hooks';
import { changePasswordSchema, type ChangePasswordForm } from '@/schemas/auth';
import { radius, spacing } from '@/theme/tokens';
import { colorFromString, initials } from '@/utils/format';
import { STR } from '@/utils/constants';
import { zodResolver } from '@/utils/zodResolver';

interface ProfileForm {
  fullName: string;
}

/**
 * Hồ sơ: avatar, sửa tên, đổi mật khẩu, lối vào lịch/thống kê, đăng xuất.
 * @reference docs/plan-app/02-phase-2.md §5.9
 */
export default function ProfileScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();

  const me = useMe();
  const updateMe = useUpdateMe();
  const uploadAvatar = useUploadAvatar();
  const changePassword = useChangePassword();
  const logout = useLogout();

  const profileForm = useForm<ProfileForm>({ defaultValues: { fullName: '' } });
  const passwordForm = useForm<ChangePasswordForm>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { oldPassword: '', newPassword: '' },
  });

  useEffect(() => {
    if (me.data) {
      profileForm.reset({ fullName: me.data.fullName });
    }
  }, [me.data, profileForm]);

  const onSaveProfile = profileForm.handleSubmit(async (values) => {
    try {
      await updateMe.mutateAsync({ fullName: values.fullName.trim() });
      show(STR.profile.saved);
    } catch (error) {
      show(normalizeError(error).message);
    }
  });

  const onChangePassword = passwordForm.handleSubmit(async (values) => {
    try {
      await changePassword.mutateAsync(values);
      show(STR.profile.passwordChanged);
      await logout.mutateAsync();
      router.replace('/login');
    } catch (error) {
      show(normalizeError(error).message);
    }
  });

  const onPickAvatar = async () => {
    try {
      const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!permission.granted) {
        show(STR.profileExtra.avatarFailed);
        return;
      }
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: true,
        aspect: [1, 1],
        quality: 0.7,
      });
      if (result.canceled || !result.assets[0]) return;
      const asset = result.assets[0];
      await uploadAvatar.mutateAsync({
        uri: asset.uri,
        name: asset.fileName ?? `avatar-${Date.now()}.jpg`,
        mimeType: asset.mimeType ?? 'image/jpeg',
      });
      show(STR.profileExtra.avatarChanged);
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  const confirmLogout = () => {
    Alert.alert(STR.profile.logout, STR.profile.logoutConfirm, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.profile.logout,
        style: 'destructive',
        onPress: async () => {
          await logout.mutateAsync();
          router.replace('/login');
        },
      },
    ]);
  };

  const header = <TabHeader title={STR.profile.title} />;

  if (me.isPending) {
    return (
      <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
        {header}
        <SkeletonList count={2} />
      </View>
    );
  }

  const avatarColor = colorFromString(me.data?.fullName);
  const avatarSource = authImageSource(me.data?.avatarUrl);

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      {header}
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.identity}>
              {avatarSource ? (
                <Avatar.Image size={82} source={avatarSource} />
              ) : (
                <Avatar.Text
                  size={82}
                  label={initials(me.data?.fullName)}
                  style={{ backgroundColor: `${avatarColor}1F` }}
                  labelStyle={[styles.avatarLabel, { color: avatarColor }]}
                />
              )}
              <AppButton
                label={uploadAvatar.isPending ? STR.common.saving : 'Đổi ảnh đại diện'}
                variant="text"
                icon="camera-outline"
                loading={uploadAvatar.isPending}
                onPress={onPickAvatar}
              />
              <Text variant="titleLarge" style={styles.name}>
                {me.data?.fullName}
              </Text>
              <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {me.data?.email}
              </Text>
            </Card.Content>
          </Card>

          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.noPadding}>
              <List.Item
                title={STR.profileExtra.mySchedule}
                left={(props) => <List.Icon {...props} icon="calendar-month-outline" />}
                right={(props) => <List.Icon {...props} icon="chevron-right" />}
                onPress={() => router.push('/schedule')}
              />
              <List.Item
                title={STR.profileExtra.myStats}
                left={(props) => <List.Icon {...props} icon="chart-bar" />}
                right={(props) => <List.Icon {...props} icon="chevron-right" />}
                onPress={() => router.push('/stats')}
              />
            </Card.Content>
          </Card>

          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <View style={styles.sectionHeader}>
                <Icon source="account-edit-outline" size={20} color={theme.colors.primary} />
                <Text variant="titleSmall">{STR.profile.accountInfo}</Text>
              </View>
              <ControlledField
                control={profileForm.control}
                name="fullName"
                label={STR.profile.fullName}
                error={profileForm.formState.errors.fullName?.message}
                autoCapitalize="words"
                maxLength={100}
                leftIcon="account-outline"
              />
              <View style={[styles.readonlyRow, { backgroundColor: theme.colors.surfaceVariant }]}>
                <Icon source="email-outline" size={18} color={theme.colors.onSurfaceVariant} />
                <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                  {me.data?.email}
                </Text>
              </View>
              <AppButton
                label={STR.profile.saveCta}
                icon="content-save-outline"
                onPress={onSaveProfile}
                loading={updateMe.isPending}
                style={styles.button}
              />
            </Card.Content>
          </Card>

          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <View style={styles.sectionHeader}>
                <Icon source="lock-reset" size={20} color={theme.colors.primary} />
                <Text variant="titleSmall">{STR.profile.changePassword}</Text>
              </View>
              <ControlledField
                control={passwordForm.control}
                name="oldPassword"
                label={STR.profile.oldPassword}
                error={passwordForm.formState.errors.oldPassword?.message}
                secure
                leftIcon="lock-outline"
              />
              <ControlledField
                control={passwordForm.control}
                name="newPassword"
                label={STR.profile.newPassword}
                error={passwordForm.formState.errors.newPassword?.message}
                helperText={STR.auth.passwordHint}
                secure
                leftIcon="lock-check-outline"
              />
              <AppButton
                label={STR.profile.changePasswordCta}
                icon="lock-reset"
                onPress={onChangePassword}
                loading={changePassword.isPending}
                style={styles.button}
              />
            </Card.Content>
          </Card>

          <AppButton
            label={STR.profile.logout}
            variant="danger"
            icon="logout"
            onPress={confirmLogout}
            loading={logout.isPending}
            style={styles.logout}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  flex: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.lg },
  noPadding: { padding: 0 },
  identity: { alignItems: 'center', gap: spacing.xs, paddingVertical: spacing.lg },
  avatarLabel: { fontSize: 28, fontWeight: '700' },
  name: { fontWeight: '700', marginTop: spacing.sm },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.md },
  readonlyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    marginTop: spacing.xs,
  },
  button: { marginTop: spacing.md },
  logout: { marginTop: spacing.sm },
});
