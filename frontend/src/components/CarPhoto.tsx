import { useEffect, useRef, useState } from 'react';
import { carsApi } from '../api/endpoints';
import { CarArt } from './ui';

// GET /images/{id} requires the JWT, which an <img> tag cannot send, so each image is fetched
// as a blob. Uploaded images never change, so each one is fetched once per page load and reused.
const cache = new Map<number, Promise<string>>();

function loadImage(imageId: number): Promise<string> {
  let url = cache.get(imageId);
  if (!url) {
    url = carsApi.image(imageId).then((blob) => URL.createObjectURL(blob));
    url.catch(() => cache.delete(imageId)); // allow a retry next time
    cache.set(imageId, url);
  }
  return url;
}

/** Fetches an image; pass undefined to skip loading (e.g. a slide that is not near the screen). */
export function useCarImage(imageId: number | undefined) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    setSrc(null);
    if (imageId === undefined) return;
    let active = true;
    loadImage(imageId)
      .then((url) => active && setSrc(url))
      .catch(() => undefined); // keep showing the placeholder
    return () => {
      active = false;
    };
  }, [imageId]);

  return src;
}

function Slide({ imageId, load, make, category }: { imageId: number; load: boolean; make: string; category: string }) {
  const src = useCarImage(load ? imageId : undefined);
  return (
    <div className="slide">
      {src ? <img src={src} alt={`${make} photo`} draggable={false} /> : <CarArt make={make} category={category} />}
    </div>
  );
}

function Thumb({ imageId, active, onClick }: { imageId: number; active: boolean; onClick: () => void }) {
  const src = useCarImage(imageId);
  return (
    <button type="button" className={`thumb ${active ? 'active' : ''}`} onClick={onClick} aria-label="Show photo">
      {src ? <img src={src} alt="" /> : <span className="thumb-loading" />}
    </button>
  );
}

interface CarouselProps {
  imageIds: number[];
  make: string;
  category: string;
  /** "card" is the compact version used in car cards. */
  variant?: 'detail' | 'card';
  /** Called on a tap/click on the photo itself (not on a swipe or an arrow). */
  onOpen?: () => void;
  /** Show the thumbnail strip under the photo. */
  thumbnails?: boolean;
}

/**
 * Swipeable photos. Swiping is native CSS scroll-snap, so touch, trackpad and mouse-wheel scrolling all
 * work; arrows, dots, thumbnails and arrow keys jump to a photo. Only the current and neighbouring
 * photos are loaded.
 */
export function CarCarousel({ imageIds, make, category, variant = 'detail', onOpen, thumbnails }: CarouselProps) {
  const track = useRef<HTMLDivElement>(null);
  const [index, setIndex] = useState(0);
  const idsKey = imageIds.join(',');

  useEffect(() => {
    setIndex(0);
    track.current?.scrollTo({ left: 0 });
  }, [idsKey]);

  const many = imageIds.length > 1;

  const goTo = (i: number) => {
    const el = track.current;
    if (!el) return;
    const next = Math.max(0, Math.min(imageIds.length - 1, i));
    el.scrollTo({ left: next * el.clientWidth, behavior: 'smooth' });
  };

  // The current photo follows the scroll position, whether it moved by swipe, arrow or thumbnail.
  const onScroll = () => {
    const el = track.current;
    if (el && el.clientWidth) setIndex(Math.round(el.scrollLeft / el.clientWidth));
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      goTo(index + (e.key === 'ArrowRight' ? 1 : -1));
    } else if (e.key === 'Enter' && onOpen) {
      onOpen();
    }
  };

  // Arrows sit on top of a clickable photo, so they must not trigger onOpen.
  const arrow = (step: number) => (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    goTo(index + step);
  };

  return (
    <div className={`gallery gallery-${variant}`}>
      <div className={`carousel ${onOpen ? 'clickable' : ''}`}>
        {imageIds.length === 0 ? (
          <div className="slide" onClick={onOpen}>
            <CarArt make={make} category={category} />
          </div>
        ) : (
          <div
            ref={track}
            className="carousel-track"
            onScroll={onScroll}
            onKeyDown={onKeyDown}
            onClick={onOpen}
            tabIndex={0}
            role="region"
            aria-label={`${make} photos, ${index + 1} of ${imageIds.length}`}
          >
            {imageIds.map((id, i) => (
              <Slide key={id} imageId={id} load={Math.abs(i - index) <= 1} make={make} category={category} />
            ))}
          </div>
        )}

        {imageIds.length > 0 && <span className="car-art-tag">{category}</span>}
        {many && (
          <>
            <span className="carousel-count">
              {index + 1} / {imageIds.length}
            </span>
            <button
              type="button"
              className="carousel-arrow prev"
              onClick={arrow(-1)}
              disabled={index === 0}
              aria-label="Previous photo"
            >
              ‹
            </button>
            <button
              type="button"
              className="carousel-arrow next"
              onClick={arrow(1)}
              disabled={index === imageIds.length - 1}
              aria-label="Next photo"
            >
              ›
            </button>
            <div className="carousel-dots" aria-hidden="true">
              {imageIds.map((id, i) => (
                <span key={id} className={i === index ? 'active' : ''} />
              ))}
            </div>
          </>
        )}
      </div>

      {thumbnails && many && (
        <div className="thumbs">
          {imageIds.map((id, i) => (
            <Thumb key={id} imageId={id} active={i === index} onClick={() => goTo(i)} />
          ))}
        </div>
      )}
    </div>
  );
}

/** Full-size carousel with thumbnails, for the car detail page. */
export function CarGallery({ imageIds, make, category }: { imageIds: number[]; make: string; category: string }) {
  return <CarCarousel imageIds={imageIds} make={make} category={category} thumbnails />;
}
