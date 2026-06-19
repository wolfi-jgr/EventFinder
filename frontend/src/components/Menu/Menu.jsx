import React from "react";
import MenuItem from "./MenuItem";
import "./Menu.css";

/**
 * Menu Component - Renders menu items inside modal
 * 
 * Props:
 *   - items (array): Array of menu item objects:
 *     {
 *       id: string (unique identifier),
 *       icon: string (emoji/unicode),
 *       label: string,
 *       type: 'button' | 'divider' | 'toggle' (default: 'button'),
 *       onClick: function,
 *       isActive: bool (for toggle items),
 *       disabled: bool
 *     }
 *   - title (string, optional): Menu title/header
 */
export default function Menu({ items, title }) {
  return (
    <div className="menu-container" role="menu">
      {title && <h2 className="menu-title">{title}</h2>}
      <div className="menu-items-list">
        {items.map((item) => {
          if (item.type === "divider") {
            return <MenuItem key={item.id} type="divider" />;
          }

          return (
            <MenuItem
              key={item.id}
              icon={item.icon}
              label={item.label}
              onClick={item.onClick}
              type={item.type}
              isActive={item.isActive}
              disabled={item.disabled}
            />
          );
        })}
      </div>
    </div>
  );
}
