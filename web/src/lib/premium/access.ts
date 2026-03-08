import { doc, getDoc } from 'firebase/firestore';
import { User } from 'firebase/auth';

import { db } from '@/lib/firebase/client';

export async function userHasPremiumAccess(user: User | null): Promise<boolean> {
  if (!user) return false;

  const token = await user.getIdTokenResult();
  if (token.claims.premium === true) return true;

  const profileRef = doc(db, 'users', user.uid);
  const profileSnap = await getDoc(profileRef);

  return profileSnap.exists() && profileSnap.data().hasPremiumAccess === true;
}
