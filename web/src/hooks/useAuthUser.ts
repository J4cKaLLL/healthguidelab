'use client';

import { onAuthStateChanged, User } from 'firebase/auth';
import { useEffect, useState } from 'react';

import { auth } from '@/lib/firebase/client';
import { userHasPremiumAccess } from '@/lib/premium/access';

type AuthState = {
  user: User | null;
  isLoading: boolean;
  hasPremiumAccess: boolean;
};

export function useAuthUser(): AuthState {
  const [user, setUser] = useState<User | null>(null);
  const [hasPremiumAccess, setHasPremiumAccess] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (nextUser) => {
      setUser(nextUser);
      setIsLoading(true);
      setHasPremiumAccess(await userHasPremiumAccess(nextUser));
      setIsLoading(false);
    });

    return unsubscribe;
  }, []);

  return { user, isLoading, hasPremiumAccess };
}
