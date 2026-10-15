import { Navigate, type RouteObject } from 'react-router';
import { PaymentDetailPage } from '@/features/payments/pages/PaymentDetailPage';
import { PaymentsListPage } from '@/features/payments/pages/PaymentsListPage';
import { AppLayout } from './layout/AppLayout';
import { NotFoundPage } from './pages/NotFoundPage';
import { RouteErrorPage } from './pages/RouteErrorPage';

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <AppLayout />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <Navigate to="/payments" replace /> },
      { path: 'payments', element: <PaymentsListPage /> },
      { path: 'payments/:paymentId', element: <PaymentDetailPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
