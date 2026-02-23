UPDATE scrape_rules SET event_item_selector = '.et_pb_portfolio_item' WHERE site_name = 'grelleforelle.com';
UPDATE scrape_rules SET event_item_selector = 'li.event, div.veranstaltung, .ct-event-item' WHERE site_name = 'daswerk.org';
UPDATE scrape_rules SET event_item_selector = '[data-cy="event"], .event-item, .event, li[data-event]' WHERE site_name = 'savedate.io/@prst';
SELECT site_name, event_item_selector FROM scrape_rules WHERE site_name IN ('grelleforelle.com', 'daswerk.org', 'savedate.io/@prst');
