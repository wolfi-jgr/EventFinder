import { useState, useRef } from "react";
import EventCard from "./EventCard";
import "./LocationSlider.css";

export default function LocationSlider({ location, events }) {
  const [scrollPosition, setScrollPosition] = useState(0);
  const scrollContainerRef = useRef(null);

  const scroll = (direction) => {
    const container = scrollContainerRef.current;
    const scrollAmount = 340; // Card width (300px) + gap (1rem = 16px) + padding
    
    if (direction === "left") {
      container.scrollBy({ left: -scrollAmount, behavior: "smooth" });
    } else {
      container.scrollBy({ left: scrollAmount, behavior: "smooth" });
    }

    // Update scroll position after animation
    setTimeout(() => {
      if (container) {
        setScrollPosition(container.scrollLeft);
      }
    }, 300);
  };

  const handleScroll = () => {
    if (scrollContainerRef.current) {
      setScrollPosition(scrollContainerRef.current.scrollLeft);
    }
  };

  const canScrollLeft = scrollPosition > 5;
  const canScrollRight =
    scrollContainerRef.current &&
    scrollPosition < scrollContainerRef.current.scrollWidth - scrollContainerRef.current.clientWidth - 5;

  return (
    <div className="location-slider">
      <div className="location-header">
        <h2 className="location-name">
          📍 {location}
          <span className="event-count">{events.length}</span>
        </h2>
      </div>

      <div className="slider-wrapper">
        {canScrollLeft && (
          <button
            className="scroll-button scroll-left"
            onClick={() => scroll("left")}
            aria-label="Scroll left"
            title="Previous events"
          >
            ◀
          </button>
        )}

        <div className="scroll-area" ref={scrollContainerRef} onScroll={handleScroll}>
          {events.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>

        {canScrollRight && (
          <button
            className="scroll-button scroll-right"
            onClick={() => scroll("right")}
            aria-label="Scroll right"
            title="Next events"
          >
            ▶
          </button>
        )}
      </div>
    </div>
  );
}
