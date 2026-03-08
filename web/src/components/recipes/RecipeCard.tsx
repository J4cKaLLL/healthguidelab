import Image from 'next/image';
import Link from 'next/link';

import { Recipe } from '@/lib/types/recipe';

type RecipeCardProps = {
  recipe: Recipe;
  hasPremiumAccess: boolean;
};

export function RecipeCard({ recipe, hasPremiumAccess }: RecipeCardProps) {
  const shouldShowLock = recipe.isPremium && !hasPremiumAccess;
  const ingredientsPreview = recipe.ingredients.slice(0, 3).join(' • ');

  return (
    <Link href={`/recipes/${recipe.id}`} className="recipe-card">
      <div className="recipe-image-wrap">
        <Image src={recipe.imageUrl} alt={recipe.title} fill className="recipe-image" sizes="(max-width: 768px) 100vw, 33vw" />
      </div>

      <div className="recipe-content">
        <h3>{recipe.title}</h3>
        <p className="recipe-category">{recipe.category}</p>
        <p className="recipe-preview"><strong>Ingredients:</strong> {ingredientsPreview}</p>
        <p className="recipe-preview"><strong>Preparation:</strong> {recipe.preparation.slice(0, 92)}...</p>

        {recipe.isPremium && (
          <span className="premium-badge" aria-label="Premium recipe">
            {shouldShowLock ? '🔒 Premium' : 'Premium'}
          </span>
        )}
      </div>
    </Link>
  );
}
