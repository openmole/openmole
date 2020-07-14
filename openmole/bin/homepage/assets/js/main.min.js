let prevScrollpos = window.pageYOffset;
const logo = document.getElementById("logo");

window.onscroll = function () {
  let currentScrollPos = window.pageYOffset;

  if (prevScrollpos > 68) {
    // Move logo to header below 68px
    logo.classList.add("isMoved");
  } else {
    logo.classList.remove("isMoved");
  }
  prevScrollpos = currentScrollPos;
};
