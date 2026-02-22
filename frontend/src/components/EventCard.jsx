import "./EventCard.css";

export default function EventCard({ event }) {
  const formatDateTime = (dateTime) => {
    if (!dateTime) return "";
    const date = new Date(dateTime);
    return date.toLocaleString('de-AT', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  };

  const formatPrice = () => {
    if (event.priceNote) return event.priceNote;
    if (event.priceFrom && event.priceTo) {
      return `€${event.priceFrom} - €${event.priceTo}`;
    }
    if (event.priceFrom) return `€${event.priceFrom}`;
    return "Price TBA";
  };

  return (
    <div className="event-card">
      <div className="event-card-header">
        <h3 className="event-card-title">{event.title}</h3>
        {event.category && <span className="event-card-badge">{event.category}</span>}
      </div>

      <div className="event-card-body">
        <div className="event-card-date">
          <span className="calendar-icon">📅</span>
          <span>{formatDateTime(event.startDateTime)}</span>
        </div>

        {event.venue && (
          <div className="event-card-venue">
            <span className="venue-icon">🎭</span>
            <span>{event.venue}</span>
          </div>
        )}

        <div className="event-card-price">
          <span className="price-icon">💰</span>
          <span>{formatPrice()}</span>
        </div>

        {event.description && (
          <p className="event-card-description">{event.description.substring(0, 100)}...</p>
        )}
      </div>

      {event.sourceUrl && (
        <div className="event-card-footer">
          <a href={event.sourceUrl} target="_blank" rel="noreferrer" className="event-card-link">
            Details →
          </a>
        </div>
      )}
    </div>
  );
}
