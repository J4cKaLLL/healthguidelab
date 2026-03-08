import Image from 'next/image';

import { Recipe } from '@/lib/types/recipe';

type RecipeDetailProps = {
  recipe: Recipe;
};

export function RecipeDetail({ recipe }: RecipeDetailProps) {
  return (
    <article className="recipe-detail">
      <div className="recipe-detail-image-wrap">
        <Image src={recipe.imageUrl} alt={recipe.title} fill className="recipe-image" sizes="100vw" priority />
      </div>

      <div className="recipe-detail-content">
        <p className="recipe-category">{recipe.category}</p>
        <h1>{recipe.title}</h1>

        <section>
          <h2>Ingredients</h2>
          <ul>
            {recipe.ingredients.map((ingredient) => (
              <li key={ingredient}>{ingredient}</li>
            ))}
          </ul>
        </section>

        <section>
          <h2>Preparation</h2>
          <p>{recipe.preparation}</p>
        </section>
      </div>
    </article>
  );
}
