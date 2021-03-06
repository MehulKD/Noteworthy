package com.subhrajyoti.babai.noteworthy.Presenters;


import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import com.subhrajyoti.babai.noteworthy.Activities.DetailsActivity;
import com.subhrajyoti.babai.noteworthy.DB.DBController;
import com.subhrajyoti.babai.noteworthy.Models.Note;
import com.subhrajyoti.babai.noteworthy.R;
import com.subhrajyoti.babai.noteworthy.Views.TrashView;

import java.util.ArrayList;
import java.util.List;

public class TrashPresenter {

    private TrashView trashView;
    private DBController dbController;

    public TrashPresenter(TrashView mainView) {
        this.trashView = mainView;
        dbController = new DBController(mainView.getContext());
    }

    public void startDetailActivity(int position, String title, String desc, View view) {
        Intent intent = new Intent(trashView.getContext(), DetailsActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("desc", desc);
        intent.putExtra("position", position);
        intent.putExtra("caller","Trash");
        String transitionName = trashView.getContext().getString(R.string.transition_name);

        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) trashView.getContext(),
                        view,
                        transitionName
                );
        ActivityCompat.startActivity( trashView.getContext(), intent,  options.toBundle());
    }



    public void getNotes() {
        trashView.showNotes(dbController.getAllNotesFromTrash());
    }

    public void closeDB() {
        dbController.close();
    }



    public void deleteNote(Note note) {
        dbController.deleteNoteFromTrash(note);
        trashView.updateNotesAfterChanges(note);
        trashView.showSnackBarDelete(note);
    }


    public void filter(List<Note> models, String query) {
        //convert query text to lower case for easier searching
        query = query.toLowerCase();
        ArrayList<Note> filteredModelList = new ArrayList<>();
        //iterate over all notes and check if they contain the query string
        for (Note model : models) {
            final String text = model.getTitle().concat(" ").concat(model.getDesc()).toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        trashView.showFilteredNotes(filteredModelList);
    }

    public void restoreNote(Note note){
        dbController.addNote(note);
        dbController.deleteNoteFromTrash(note);
        trashView.updateNotesAfterChanges(note);
        trashView.showSnackBarRestore(note);
    }




}
