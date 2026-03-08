import {
  Timestamp,
  QueryConstraint,
  addDoc,
  collection,
  doc,
  DocumentData,
  getDoc,
  getDocs,
  orderBy,
  query,
  serverTimestamp,
  where
} from 'firebase/firestore';

import { db } from '@/lib/firebase/client';
import { Recipe, RecipeCreateInput } from '@/lib/types/recipe';

const RECIPES_COLLECTION = 'recipes';

function normalizeRecipe(id: string, data: DocumentData): Recipe {
  return {
    id,
    title: String(data.title ?? ''),
    ingredients: Array.isArray(data.ingredients) ? data.ingredients.map(String) : [],
    preparation: String(data.preparation ?? ''),
    imageUrl: String(data.imageUrl ?? ''),
    isPremium: Boolean(data.isPremium),
    category: String(data.category ?? 'general').toLowerCase(),
    createdAt: data.createdAt as Timestamp | undefined
  };
}

export async function getRecipes(category?: string): Promise<Recipe[]> {
  const recipesRef = collection(db, RECIPES_COLLECTION);
  const constraints: QueryConstraint[] = [orderBy('createdAt', 'desc')];

  if (category && category !== 'all') {
    constraints.unshift(where('category', '==', category.toLowerCase()));
  }

  const recipesQuery = query(recipesRef, ...constraints);
  const snapshot = await getDocs(recipesQuery);

  return snapshot.docs.map((recipeDoc) => normalizeRecipe(recipeDoc.id, recipeDoc.data()));
}

export async function getRecipeById(id: string): Promise<Recipe | null> {
  const recipeRef = doc(db, RECIPES_COLLECTION, id);
  const snapshot = await getDoc(recipeRef);

  if (!snapshot.exists()) return null;
  return normalizeRecipe(snapshot.id, snapshot.data());
}

export async function createRecipe(recipe: RecipeCreateInput): Promise<string> {
  const recipesRef = collection(db, RECIPES_COLLECTION);
  const docRef = await addDoc(recipesRef, {
    ...recipe,
    category: recipe.category.toLowerCase(),
    createdAt: serverTimestamp()
  });

  return docRef.id;
}
