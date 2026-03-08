import { Timestamp } from 'firebase/firestore';

export type Recipe = {
  id: string;
  title: string;
  ingredients: string[];
  preparation: string;
  imageUrl: string;
  isPremium: boolean;
  category: string;
  createdAt?: Timestamp;
};

export type RecipeCreateInput = Omit<Recipe, 'id' | 'createdAt'>;
