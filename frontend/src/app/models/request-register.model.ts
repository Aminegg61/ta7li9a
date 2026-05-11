export interface RequestRegister {
  firstName: string;
  lastName: string;
  password: string;
  confirmPassword: string;
  phoneNumber: string;
  role: 'CLIENT' | 'COIFFEUR';
}

export interface RequestLogin{
  phone:string;
  password:string;
}
