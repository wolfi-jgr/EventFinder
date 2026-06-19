import React, { useState } from "react";
import Modal from "../Modal/Modal";
import Menu from "./Menu";
import "./MenuButton.css";

/**
 * MenuButton Component - Button with modal menu
 * 
 * Props:
 *   - menuItems (array): Array of menu item objects to pass to Menu component
 *   - menuTitle (string, optional): Title to display in the menu
 */
export default function MenuButton({ menuItems, menuTitle }) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const handleMenuItemClick = (onClick) => {
    if (onClick) {
      onClick();
    }
    setIsMenuOpen(false);
  }

  const menuItemsWithClickHandlers = menuItems.map((item) => ({
    ...item,
    onClick: item.onClick ? () => handleMenuItemClick(item.onClick) : undefined,
  }));

  return (
    <>
      <button
        className="menu-button"
        onClick={() => setIsMenuOpen(true)}
        aria-label="Open menu"
        title="Open menu"
        aria-expanded={isMenuOpen}
        aria-controls="main-menu"
      >
        <span className="menu-button-icon">≡</span>
      </button>

      <Modal isOpen={isMenuOpen} onClose={() => setIsMenuOpen(false)}>
        <Menu items={menuItemsWithClickHandlers} title={menuTitle} />
      </Modal>
    </>
  );
}
