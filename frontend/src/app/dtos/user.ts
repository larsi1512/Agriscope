export interface UserProfileDetail {
  email: string;
  firstName: string;
  lastName: string;
  profilePicture: string | null;
}

export interface EditUserDto {
  firstName: string;
  lastName: string;
  oldPassword?: string;
  newPassword?: string;
  profilePicture?: string;
}

export interface ResetPasswordDto {
  password: string;
  confirmPassword: string;
}
