import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { filesApi, type UploadableFile } from '../../api/files';
import { usersApi } from '../../api/users';
import { useSession } from '../../store/session';

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: usersApi.me });
}

export function useUpdateMe() {
  const { updateUser } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: { fullName?: string }) => usersApi.updateMe(payload),
    onSuccess: async (user) => {
      await updateUser(user);
      await queryClient.invalidateQueries({ queryKey: ['me'] });
    },
  });
}

export function useUploadAvatar() {
  const { updateUser } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file: UploadableFile) => filesApi.uploadAvatar(file),
    onSuccess: async (user) => {
      await updateUser(user);
      await queryClient.invalidateQueries({ queryKey: ['me'] });
    },
  });
}
