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
    let isMounted = true;

    const unsubscribe = onAuthStateChanged(auth, async (nextUser) => {
      if (!isMounted) return;

      setUser(nextUser);
      setIsLoading(true);

      try {
        const premium = await userHasPremiumAccess(nextUser);
        if (isMounted) setHasPremiumAccess(premium);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    });

    return () => {
      isMounted = false;
      unsubscribe();
    };
  }, []);

  return { user, isLoading, hasPremiumAccess };
}
