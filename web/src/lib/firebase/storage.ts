import { getDownloadURL, ref, uploadBytes } from 'firebase/storage';

import { storage } from '@/lib/firebase/client';

const RECIPE_IMAGES_FOLDER = 'recipes-images';

export async function uploadRecipeImage(file: File, fileName: string): Promise<string> {
  const storageRef = ref(storage, `${RECIPE_IMAGES_FOLDER}/${fileName}`);
  await uploadBytes(storageRef, file, { contentType: file.type });

  return getDownloadURL(storageRef);
}
