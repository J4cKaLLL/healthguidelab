import {
  collection,
  doc,
  DocumentData,
  getDoc,
  getDocs,
  orderBy,
  query
} from 'firebase/firestore';

import { db } from '@/lib/firebase/client';
import { Recipe } from '@/lib/types/recipe';

const RECIPES_COLLECTION = 'recipes';

const toRecipe = (id: string, data: DocumentData): Recipe => ({
  id,
  title: data.title,
  ingredients: data.ingredients ?? [],
  preparation: data.preparation,
  imageUrl: data.imageUrl,
  isPremium: Boolean(data.isPremium),
  category: data.category,
  createdAt: data.createdAt
});

export async function getRecipes(): Promise<Recipe[]> {
  const recipesRef = collection(db, RECIPES_COLLECTION);
  const recipesQuery = query(recipesRef, orderBy('createdAt', 'desc'));
  const snapshot = await getDocs(recipesQuery);

  return snapshot.docs.map((recipeDoc) => toRecipe(recipeDoc.id, recipeDoc.data()));
}

export async function getRecipeById(id: string): Promise<Recipe | null> {
  const recipeRef = doc(db, RECIPES_COLLECTION, id);
  const snapshot = await getDoc(recipeRef);

  if (!snapshot.exists()) return null;
  return toRecipe(snapshot.id, snapshot.data());
}
