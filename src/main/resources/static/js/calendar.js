/*
 * Calendario de reservas sobre FullCalendar 5.11.3 (la misma librería y versión del sistema Laravel).
 * Pinta cada día según su estado: disponible, cerrado (#ccccff como el original), lleno o fuera de plazo.
 *
 *   const cal = BbqCalendar.create(el, {
 *     loadDays: (start, end) => Api.get(...calendar-days...),   // {minDate, maxDate, closedDates, fullDates}
 *     loadEvents: (start, end) => Api.get(...),                // opcional: eventos (reservas)
 *     onDayClick: (dateStr, state) => {},                      // state: available | closed | full | out
 *     onEventClick: (event) => {},
 *     withList: true,                                          // botón "Lista" (vista listMonth)
 *   });
 */
const BbqCalendar = (() => {
  const STATES = ['available', 'closed', 'full', 'out'];

  function legendHtml(withEvents) {
    return `
      <div class="legend">
        <span><i style="background:#fff"></i> Disponible</span>
        <span><i style="background:#ccccff"></i> Cerrado</span>
        <span><i style="background:#ffebee"></i> Lleno</span>
        <span><i style="background:#f5f5f5"></i> Fuera del plazo de reserva</span>
        ${withEvents ? `
        <span><i style="background:${Ui.SALE_STATUS[1].color}"></i> Pendiente de pago</span>
        <span><i style="background:${Ui.SALE_STATUS[2].color}"></i> Pagada</span>` : ''}
      </div>`;
  }

  function create(el, { loadDays, loadEvents, onDayClick, onEventClick, withList = false }) {
    let days = { closed: new Set(), full: new Set(), min: null, max: null };
    let selected = null;

    function stateOf(dateStr) {
      if (days.closed.has(dateStr)) return 'closed';
      if (days.full.has(dateStr)) return 'full';
      if (!days.min || dateStr < days.min || dateStr > days.max) return 'out';
      return 'available';
    }

    function paintCell(cell) {
      const d = cell.dataset.date;
      if (!d) return;
      STATES.forEach((s) => cell.classList.remove('day-' + s));
      cell.classList.add('day-' + stateOf(d));
      cell.classList.toggle('day-selected', d === selected);
    }

    function paint() {
      el.querySelectorAll('.fc-daygrid-day[data-date]').forEach(paintCell);
    }

    async function reloadDays(start, end) {
      try {
        const r = await loadDays(start, end);
        days = { closed: new Set(r.closedDates), full: new Set(r.fullDates), min: r.minDate, max: r.maxDate };
        paint();
      } catch (err) {
        Ui.flash(err.message, 'error');
      }
    }

    const calendar = new FullCalendar.Calendar(el, {
      initialView: 'dayGridMonth',
      locale: 'es',
      headerToolbar: { left: 'prev,next today', center: 'title', right: withList ? 'dayGridMonth,listMonth' : '' },
      buttonText: { today: 'Hoy', month: 'Mes', list: 'Lista' },
      noEventsContent: 'No hay reservas en este mes',
      fixedWeekCount: false,
      showNonCurrentDates: true,
      height: 'auto',
      editable: false,
      eventDisplay: 'block',
      displayEventTime: false,
      events: loadEvents
        ? (info, success, failure) => {
            loadEvents(Ui.isoDate(info.start), Ui.isoDate(info.end))
              .then((events) => success(events.map((e) => ({
                ...e,
                color: (Ui.SALE_STATUS[e.status] || {}).color,
              }))))
              .catch((err) => { Ui.flash(err.message, 'error'); failure(err); });
          }
        : [],
      datesSet: (info) => reloadDays(Ui.isoDate(info.start), Ui.isoDate(info.end)),
      dayCellDidMount: (arg) => paintCell(arg.el),
      dateClick: (info) => { if (onDayClick) onDayClick(info.dateStr, stateOf(info.dateStr)); },
      eventClick: (info) => { if (onEventClick) onEventClick(info.event); },
    });
    calendar.render();

    return {
      calendar,
      stateOf,
      select(dateStr) { selected = dateStr; paint(); },
      /** Recarga eventos y estado de los días (después de crear/cancelar una reserva). */
      refresh() {
        calendar.refetchEvents();
        const v = calendar.view;
        reloadDays(Ui.isoDate(v.activeStart), Ui.isoDate(v.activeEnd));
      },
    };
  }

  /** Mensaje para un día no seleccionable. */
  function stateMessage(state) {
    return {
      closed: 'La ubicación está cerrada ese día.',
      full: 'Ese día ya no tiene cupos.',
      out: 'Solo se puede reservar con 2 a 60 días de anticipación.',
    }[state] || '';
  }

  return { create, legendHtml, stateMessage };
})();
