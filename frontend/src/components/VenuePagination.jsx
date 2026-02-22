import LocationSlider from "./LocationSlider";
import "./VenuePagination.css";

export default function VenuePagination({
  locations,
  currentPage,
  totalPages,
  totalLocations,
  onNextPage,
  onPreviousPage,
}) {
  const itemsPerPage = locations.length > 0 ? 3 : 1;
  const startIndex = (currentPage - 1) * itemsPerPage + 1;
  const endIndex = Math.min(currentPage * itemsPerPage, totalLocations);

  return (
    <div className="venue-pagination">
      {/* Pagination header */}
      {totalLocations > 0 && (
        <div className="pagination-header">
          <span className="pagination-info">
            Destinations {startIndex}–{endIndex} of {totalLocations}
          </span>
        </div>
      )}

      {/* Location sliders for current page */}
      <div className="locations-container">
        {locations.length > 0 ? (
          locations.map(([location, events]) => (
            <LocationSlider key={location} location={location} events={events} />
          ))
        ) : (
          <div className="no-locations">
            <p>No event locations available yet. Try running a scrape.</p>
          </div>
        )}
      </div>

      {/* Pagination controls */}
      {totalPages > 1 && (
        <div className="pagination-controls">
          <button
            className="pagination-button pagination-prev"
            onClick={onPreviousPage}
            disabled={currentPage === 1}
            aria-label="Previous page"
          >
            ← Previous
          </button>

          <div className="page-indicator">
            <span className="page-number">{currentPage}</span>
            <span className="page-separator">/</span>
            <span className="page-total">{totalPages}</span>
          </div>

          <button
            className="pagination-button pagination-next"
            onClick={onNextPage}
            disabled={currentPage === totalPages}
            aria-label="Next page"
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
