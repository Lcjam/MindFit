import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { signup } from '../../api/auth'
import { useAuthStore } from '../../store/authStore'
import { SignupRequest, UserRole } from '../../types/auth'

const schema = z.object({
  email: z.string().min(1, '이메일을 입력해주세요').email('올바른 이메일 형식이 아닙니다'),
  password: z.string().min(8, '비밀번호는 8자 이상이어야 합니다').max(20, '비밀번호는 20자 이하여야 합니다'),
  name: z.string().min(1, '이름을 입력해주세요'),
  birthDate: z.string().optional(),
  role: z.enum(['ROLE_CLIENT', 'ROLE_COUNSELOR'] as const, { error: '역할을 선택해주세요' }),
})

type FormValues = z.infer<typeof schema>

export default function SignupPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'ROLE_CLIENT' },
  })

  const mutation = useMutation({
    mutationFn: (data: SignupRequest) => signup(data),
    onSuccess: (response) => {
      setAuth(response)
      navigate('/')
    },
  })

  const onSubmit = (data: FormValues) => {
    const payload: SignupRequest = {
      email: data.email,
      password: data.password,
      name: data.name,
      role: data.role as UserRole,
      ...(data.birthDate ? { birthDate: data.birthDate } : {}),
    }
    mutation.mutate(payload)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md bg-white rounded-xl shadow p-8">
        <h1 className="text-2xl font-bold text-center mb-6 text-gray-900">회원가입</h1>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
            <input
              id="email"
              type="email"
              {...register('email')}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="example@email.com"
            />
            {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
            <input
              id="password"
              type="password"
              {...register('password')}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="8자 이상 20자 이하"
            />
            {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          </div>
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">이름</label>
            <input
              id="name"
              type="text"
              {...register('name')}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}
          </div>
          <div>
            <label htmlFor="birthDate" className="block text-sm font-medium text-gray-700 mb-1">생년월일 (선택)</label>
            <input
              id="birthDate"
              type="date"
              {...register('birthDate')}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <p className="block text-sm font-medium text-gray-700 mb-2">역할</p>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="radio" value="ROLE_CLIENT" {...register('role')} />
                <span className="text-sm">내담자</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="radio" value="ROLE_COUNSELOR" {...register('role')} />
                <span className="text-sm">상담사</span>
              </label>
            </div>
            {errors.role && <p className="text-red-500 text-xs mt-1">{errors.role.message}</p>}
          </div>
          {mutation.isError && (
            <p className="text-red-500 text-sm text-center">회원가입 중 오류가 발생했습니다. 다시 시도해주세요.</p>
          )}
          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {mutation.isPending ? '처리 중...' : '회원가입'}
          </button>
        </form>
        <p className="text-center text-sm text-gray-500 mt-4">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">로그인</Link>
        </p>
      </div>
    </div>
  )
}
